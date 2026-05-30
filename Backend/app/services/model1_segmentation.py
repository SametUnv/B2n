from __future__ import annotations

import threading
import time
from contextlib import nullcontext
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import torch
from torchvision.models.segmentation import lraspp_mobilenet_v3_large

from app.config import Settings
from app.services.geometry import (
    clean_mask,
    crop_bbox,
    detect_quad,
    draw_quad,
    letterbox_rgb,
    mask_to_gray,
    overlay_mask,
    probability_to_gray,
    unletterbox_probability,
    warp_perspective,
)


@dataclass
class Model1Result:
    bbox: tuple[int, int, int, int]
    quad: np.ndarray
    confidence: float
    strategy: str
    mask: np.ndarray
    mask_probability: np.ndarray
    mask_image: np.ndarray
    overlay: np.ndarray
    bbox_crop: np.ndarray
    perspective_crop: np.ndarray
    warnings: list[str]
    timings_ms: dict[str, float]


class Model1SegmentationService:
    def __init__(self, settings: Settings):
        self.settings = settings
        self._model: torch.nn.Module | None = None
        self._checkpoint: dict | None = None
        self._lock = threading.Lock()

    @property
    def model_path(self) -> Path:
        return self.settings.model1_path

    def metadata(self) -> dict:
        checkpoint = self._checkpoint
        return {
            "id": "model1_board_segmentation",
            "format": "pytorch_checkpoint",
            "path": str(self.model_path),
            "exists": self.model_path.exists(),
            "input_shape": [1, 3, self.settings.model1_image_size, self.settings.model1_image_size],
            "output_names": ["mask_logits"],
            "checkpoint_loaded": self._model is not None,
            "device": self.settings.device,
            "use_amp": self.settings.use_amp,
            "metrics": checkpoint.get("metrics") if isinstance(checkpoint, dict) else None,
        }

    def detect(self, image_rgb: np.ndarray, threshold: float = 0.5) -> Model1Result:
        threshold = float(np.clip(threshold, 0.05, 0.95))
        timings: dict[str, float] = {}
        warnings: list[str] = []

        t0 = time.perf_counter()
        model = self._load_model()
        timings["model_load_ms"] = (time.perf_counter() - t0) * 1000.0

        t0 = time.perf_counter()
        letterboxed, meta = letterbox_rgb(
            image_rgb,
            size=self.settings.model1_image_size,
            pad_value=self.settings.model1_pad_value,
        )
        tensor = torch.from_numpy(letterboxed.astype(np.float32) / 255.0).permute(2, 0, 1).unsqueeze(0).contiguous()
        tensor = tensor.to(self.settings.device)
        timings["preprocess_ms"] = (time.perf_counter() - t0) * 1000.0

        t0 = time.perf_counter()
        with torch.inference_mode():
            with self._autocast():
                output = model(tensor)
            logits = output["out"] if isinstance(output, dict) else output
            probability_640 = torch.sigmoid(logits[0, 0]).detach().cpu().numpy().astype(np.float32)
        self._sync_device()
        timings["inference_ms"] = (time.perf_counter() - t0) * 1000.0

        t0 = time.perf_counter()
        probability = unletterbox_probability(probability_640, meta)
        mask = clean_mask(probability, threshold)
        quad_detection = detect_quad(mask)
        if quad_detection.confidence < 0.7:
            warnings.append("Board boundary was found with low confidence; review the crop before OCR.")
        if quad_detection.strategy.endswith("fallback"):
            warnings.append("Automatic board contour was weak; fallback geometry was used.")

        bbox_crop = crop_bbox(image_rgb, quad_detection.bbox)
        try:
            perspective_crop = warp_perspective(
                image_rgb,
                quad_detection.quad,
                max_output_side=self.settings.perspective_max_output_side,
            )
        except Exception:
            perspective_crop = bbox_crop
            warnings.append("Perspective correction failed; bounding-box crop was used.")

        overlay = draw_quad(overlay_mask(image_rgb, mask), quad_detection.quad)
        timings["postprocess_ms"] = (time.perf_counter() - t0) * 1000.0

        return Model1Result(
            bbox=quad_detection.bbox,
            quad=quad_detection.quad,
            confidence=quad_detection.confidence,
            strategy=quad_detection.strategy,
            mask=mask,
            mask_probability=probability,
            mask_image=mask_to_gray(mask),
            overlay=overlay,
            bbox_crop=bbox_crop,
            perspective_crop=perspective_crop,
            warnings=warnings,
            timings_ms=timings,
        )

    def _load_model(self) -> torch.nn.Module:
        if self._model is not None:
            return self._model
        with self._lock:
            if self._model is not None:
                return self._model
            if not self.model_path.exists():
                raise FileNotFoundError(f"Model 1 checkpoint not found: {self.model_path}")
            checkpoint = torch.load(self.model_path, map_location="cpu", weights_only=False)
            state = self._extract_state(checkpoint)
            model = lraspp_mobilenet_v3_large(weights=None, weights_backbone=None, num_classes=1)
            model.load_state_dict(state, strict=True)
            model.to(self.settings.device)
            model.eval()
            self._checkpoint = checkpoint if isinstance(checkpoint, dict) else None
            self._model = model
            return model

    def warmup(self) -> dict[str, float]:
        model = self._load_model()
        tensor = torch.zeros(
            (1, 3, self.settings.model1_image_size, self.settings.model1_image_size),
            device=self.settings.device,
        )
        timings = {}
        for index in range(2):
            started = time.perf_counter()
            with torch.inference_mode():
                with self._autocast():
                    model(tensor)
            self._sync_device()
            timings[f"model1_warmup_pass_{index + 1}_ms"] = (time.perf_counter() - started) * 1000.0
        return timings

    def _autocast(self):
        if self.settings.use_amp:
            return torch.autocast(device_type="cuda", dtype=torch.float16)
        return nullcontext()

    def _sync_device(self) -> None:
        if self.settings.device.startswith("cuda"):
            torch.cuda.synchronize(torch.device(self.settings.device))

    @staticmethod
    def _extract_state(checkpoint):
        if isinstance(checkpoint, dict):
            for key in ("ema", "model", "state_dict", "params_ema", "params", "net", "weights"):
                value = checkpoint.get(key)
                if isinstance(value, dict):
                    return {k.replace("_orig_mod.", "", 1): v for k, v in value.items()}
        return checkpoint
