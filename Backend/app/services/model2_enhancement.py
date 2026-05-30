from __future__ import annotations

import threading
import time
from contextlib import nullcontext
from dataclasses import dataclass
from pathlib import Path

import cv2
import numpy as np
import torch

from app.config import Settings
from app.models.nafnet import NAFNetMultiHead
from app.services.geometry import probability_to_gray


@dataclass
class Model2Result:
    model_input: np.ndarray
    restored: np.ndarray
    ocr_enhanced: np.ndarray
    text_mask: np.ndarray
    text_mask_image: np.ndarray
    white_canvas: np.ndarray
    warnings: list[str]
    timings_ms: dict[str, float]


class Model2EnhancementService:
    def __init__(self, settings: Settings):
        self.settings = settings
        self._model: torch.nn.Module | None = None
        self._checkpoint: dict | None = None
        self._lock = threading.Lock()

    @property
    def model_path(self) -> Path:
        return self.settings.model2_path

    def metadata(self) -> dict:
        checkpoint = self._checkpoint
        return {
            "id": "model2_enhancement",
            "format": "pytorch_checkpoint",
            "path": str(self.model_path),
            "exists": self.model_path.exists(),
            "input_shape": [1, 3, self.settings.model2_tile_size, self.settings.model2_tile_size],
            "output_names": ["restored", "ocr", "mask_logits"],
            "tile": self.settings.model2_tile_size,
            "overlap": self.settings.model2_overlap,
            "batch_size": self.settings.model2_batch_size,
            "checkpoint_loaded": self._model is not None,
            "device": self.settings.device,
            "use_amp": self.settings.use_amp,
            "metrics": checkpoint.get("test_metrics") if isinstance(checkpoint, dict) else None,
        }

    def enhance(self, image_rgb: np.ndarray) -> Model2Result:
        timings: dict[str, float] = {}
        warnings: list[str] = []

        t0 = time.perf_counter()
        model = self._load_model()
        timings["model_load_ms"] = (time.perf_counter() - t0) * 1000.0

        t0 = time.perf_counter()
        model_input = self._scale_for_model(image_rgb)
        if model_input.shape[:2] != image_rgb.shape[:2]:
            warnings.append(
                f"Model 2 input was resized from {image_rgb.shape[1]}x{image_rgb.shape[0]} "
                f"to {model_input.shape[1]}x{model_input.shape[0]}."
            )
        input01 = model_input.astype(np.float32) / 255.0
        timings["preprocess_ms"] = (time.perf_counter() - t0) * 1000.0

        t0 = time.perf_counter()
        restored01, ocr01, mask = self._tiled_infer(model, input01)
        timings["inference_ms"] = (time.perf_counter() - t0) * 1000.0

        t0 = time.perf_counter()
        restored = to_uint8(restored01)
        ocr_enhanced = to_uint8(ocr01)
        text_mask_image = probability_to_gray(mask)
        white_canvas = make_white_canvas(ocr01, mask)
        timings["postprocess_ms"] = (time.perf_counter() - t0) * 1000.0

        return Model2Result(
            model_input=model_input,
            restored=restored,
            ocr_enhanced=ocr_enhanced,
            text_mask=mask,
            text_mask_image=text_mask_image,
            white_canvas=white_canvas,
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
                raise FileNotFoundError(f"Model 2 checkpoint not found: {self.model_path}")
            checkpoint = torch.load(self.model_path, map_location="cpu", weights_only=False)
            cfg = checkpoint.get("cfg", {}) if isinstance(checkpoint, dict) else {}
            model = NAFNetMultiHead(
                img_channel=3,
                width=int(cfg.get("width", 64)),
                middle_blk_num=int(cfg.get("mid", 12)),
                enc_blk_nums=tuple(cfg.get("enc", [2, 2, 4, 8])),
                dec_blk_nums=tuple(cfg.get("dec", [2, 2, 2, 2])),
            )
            state = checkpoint["model"] if isinstance(checkpoint, dict) and "model" in checkpoint else checkpoint
            model.load_state_dict({k.replace("_orig_mod.", "", 1): v for k, v in state.items()}, strict=True)
            model.to(self.settings.device)
            model.eval()
            self._checkpoint = checkpoint if isinstance(checkpoint, dict) else None
            self._model = model
            return model

    def _scale_for_model(self, image_rgb: np.ndarray) -> np.ndarray:
        h, w = image_rgb.shape[:2]
        longest = max(h, w)
        max_side = self.settings.model2_max_input_side
        if longest <= max_side:
            return image_rgb
        scale = max_side / float(longest)
        new_w = max(1, int(round(w * scale)))
        new_h = max(1, int(round(h * scale)))
        return cv2.resize(image_rgb, (new_w, new_h), interpolation=cv2.INTER_AREA)

    def _tiled_infer(self, model: torch.nn.Module, image01: np.ndarray) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
        tile = self.settings.model2_tile_size
        overlap = self.settings.model2_overlap
        height, width = image01.shape[:2]
        xs = tile_coords(width, tile, overlap)
        ys = tile_coords(height, tile, overlap)

        restored = np.zeros((height, width, 3), dtype=np.float32)
        ocr = np.zeros((height, width, 3), dtype=np.float32)
        mask = np.zeros((height, width), dtype=np.float32)
        acc = np.zeros((height, width), dtype=np.float32)

        tiles = []
        for y0 in ys:
            for x0 in xs:
                patch_h = min(tile, height - y0)
                patch_w = min(tile, width - x0)
                tile_input = np.empty((tile, tile, 3), dtype=np.float32)
                tile_input[:] = image01[min(y0, height - 1), min(x0, width - 1)]
                tile_input[:patch_h, :patch_w] = image01[y0 : y0 + patch_h, x0 : x0 + patch_w]
                if patch_h < tile:
                    tile_input[patch_h:, :patch_w] = tile_input[patch_h - 1 : patch_h, :patch_w]
                if patch_w < tile:
                    tile_input[:, patch_w:] = tile_input[:, patch_w - 1 : patch_w]

                tiles.append((x0, y0, patch_w, patch_h, tile_input))

        batch_size = max(1, self.settings.model2_batch_size)
        for start in range(0, len(tiles), batch_size):
            chunk = tiles[start : start + batch_size]
            batch_np = np.stack([item[4] for item in chunk], axis=0)
            tensor = torch.from_numpy(batch_np).permute(0, 3, 1, 2).contiguous().to(self.settings.device)
            with torch.inference_mode():
                with self._autocast():
                    out = model(tensor)
                restored_batch = out["restored"].clamp(0, 1).permute(0, 2, 3, 1).detach().cpu().numpy()
                ocr_batch = out["ocr"].clamp(0, 1).permute(0, 2, 3, 1).detach().cpu().numpy()
                mask_batch = torch.sigmoid(out["mask_logits"]).detach().cpu().numpy()[:, 0]
            self._sync_device()

            for idx, (x0, y0, patch_w, patch_h, _tile_input) in enumerate(chunk):
                restored_tile = restored_batch[idx]
                ocr_tile = ocr_batch[idx]
                mask_tile = mask_batch[idx]
                weight = blend_window(patch_h, patch_w, overlap)
                restored[y0 : y0 + patch_h, x0 : x0 + patch_w] += restored_tile[:patch_h, :patch_w] * weight[..., None]
                ocr[y0 : y0 + patch_h, x0 : x0 + patch_w] += ocr_tile[:patch_h, :patch_w] * weight[..., None]
                mask[y0 : y0 + patch_h, x0 : x0 + patch_w] += mask_tile[:patch_h, :patch_w] * weight
                acc[y0 : y0 + patch_h, x0 : x0 + patch_w] += weight

        acc = np.maximum(acc, 1e-6)
        return (
            np.clip(restored / acc[..., None], 0, 1),
            np.clip(ocr / acc[..., None], 0, 1),
            np.clip(mask / acc, 0, 1),
        )

    def warmup(self) -> dict[str, float]:
        model = self._load_model()
        tile = self.settings.model2_tile_size
        batch = max(1, self.settings.model2_batch_size)
        tensor = torch.zeros((batch, 3, tile, tile), device=self.settings.device)
        timings = {}
        for index in range(2):
            started = time.perf_counter()
            with torch.inference_mode():
                with self._autocast():
                    model(tensor)
            self._sync_device()
            timings[f"model2_warmup_pass_{index + 1}_ms"] = (time.perf_counter() - started) * 1000.0
        return timings

    def _autocast(self):
        if self.settings.use_amp:
            return torch.autocast(device_type="cuda", dtype=torch.float16)
        return nullcontext()

    def _sync_device(self) -> None:
        if self.settings.device.startswith("cuda"):
            torch.cuda.synchronize(torch.device(self.settings.device))


def tile_coords(length: int, tile: int, overlap: int) -> list[int]:
    if length <= tile:
        return [0]
    step = tile - overlap
    coords = list(range(0, max(1, length - tile + 1), step))
    last = length - tile
    if coords[-1] != last:
        coords.append(last)
    return sorted(set(coords))


def blend_window(height: int, width: int, overlap: int) -> np.ndarray:
    edge = min(overlap, height // 2, width // 2)
    if edge <= 0:
        return np.ones((height, width), dtype=np.float32)

    wy = np.ones(height, dtype=np.float32)
    wx = np.ones(width, dtype=np.float32)
    ramp = np.linspace(1.0 / (edge + 1), edge / (edge + 1), edge, dtype=np.float32)
    wy[:edge] = ramp
    wy[-edge:] = ramp[::-1]
    wx[:edge] = ramp
    wx[-edge:] = ramp[::-1]
    return np.outer(wy, wx).astype(np.float32)


def to_uint8(image01: np.ndarray) -> np.ndarray:
    return np.clip(image01 * 255.0, 0, 255).round().astype(np.uint8)


def make_white_canvas(ocr01: np.ndarray, mask: np.ndarray, threshold: float = 0.45) -> np.ndarray:
    alpha = (mask > threshold).astype(np.float32)
    alpha = cv2.GaussianBlur(alpha, (5, 5), 0)
    white = np.ones_like(ocr01, dtype=np.float32)
    canvas = white * (1.0 - alpha[..., None]) + np.clip(ocr01, 0, 1) * alpha[..., None]
    return to_uint8(canvas)
