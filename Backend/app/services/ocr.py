from __future__ import annotations

import threading
import time
import os
from dataclasses import dataclass
from typing import Any

import numpy as np

from app.config import Settings


@dataclass
class OcrLineResult:
    text: str
    confidence: float | None = None
    bbox: tuple[int, int, int, int] | None = None


@dataclass
class OcrResult:
    text: str
    lines: list[OcrLineResult]
    warnings: list[str]
    elapsed_ms: float


class PaddleOcrService:
    def __init__(self, settings: Settings):
        self.settings = settings
        self._engine = None
        self._lock = threading.Lock()

    def recognize(self, image_rgb: np.ndarray) -> OcrResult:
        started = time.perf_counter()
        warnings: list[str] = []
        try:
            engine = self._load_engine()
            output = self._run_engine(engine, image_rgb)
            lines = parse_paddle_output(output)
            text = "\n".join(line.text for line in lines if line.text.strip())
            if not text.strip():
                warnings.append("OCR returned no text.")
            return OcrResult(text=text, lines=lines, warnings=warnings, elapsed_ms=(time.perf_counter() - started) * 1000.0)
        except Exception as exc:
            warnings.append(f"OCR failed: {exc}")
            return OcrResult(text="", lines=[], warnings=warnings, elapsed_ms=(time.perf_counter() - started) * 1000.0)

    def _load_engine(self):
        if self._engine is not None:
            return self._engine
        with self._lock:
            if self._engine is not None:
                return self._engine
            os.environ.setdefault("FLAGS_use_mkldnn", "0")
            os.environ.setdefault("FLAGS_use_onednn", "0")
            try:
                import paddle

                paddle.set_flags({"FLAGS_use_mkldnn": False})
            except Exception:
                pass
            from paddleocr import PaddleOCR

            self._engine = PaddleOCR(
                lang=self.settings.ocr_lang,
                use_doc_orientation_classify=False,
                use_doc_unwarping=False,
                use_textline_orientation=False,
                enable_mkldnn=False,
                cpu_threads=4,
            )
            return self._engine

    @staticmethod
    def _run_engine(engine, image_rgb: np.ndarray):
        if hasattr(engine, "predict"):
            return engine.predict(image_rgb)
        return engine.ocr(image_rgb)


def parse_paddle_output(output: Any) -> list[OcrLineResult]:
    if output is None:
        return []

    items = output if isinstance(output, list) else [output]
    if len(items) == 1:
        first = as_mapping(items[0])
        if first:
            parsed = parse_mapping(first)
            if parsed:
                return parsed

    old_style = parse_old_style(items)
    if old_style:
        return old_style

    lines: list[OcrLineResult] = []
    for item in items:
        mapping = as_mapping(item)
        if mapping:
            lines.extend(parse_mapping(mapping))
    return lines


def as_mapping(item: Any) -> dict[str, Any] | None:
    if isinstance(item, dict):
        return item
    for attr in ("json", "res", "data"):
        value = getattr(item, attr, None)
        if isinstance(value, dict):
            return value
        if callable(value):
            try:
                returned = value()
                if isinstance(returned, dict):
                    return returned
            except Exception:
                pass
    try:
        return dict(item)
    except Exception:
        return None


def parse_mapping(data: dict[str, Any]) -> list[OcrLineResult]:
    if "res" in data and isinstance(data["res"], dict):
        data = data["res"]
    texts = data.get("rec_texts") or data.get("texts") or data.get("text")
    scores = data.get("rec_scores") or data.get("scores")
    boxes = data.get("rec_polys") or data.get("dt_polys") or data.get("boxes")
    if isinstance(texts, str):
        return [OcrLineResult(text=texts, confidence=float(scores) if isinstance(scores, (int, float)) else None)]
    if not isinstance(texts, (list, tuple)):
        return []
    lines: list[OcrLineResult] = []
    for index, text in enumerate(texts):
        score = None
        if isinstance(scores, (list, tuple)) and index < len(scores):
            try:
                score = float(scores[index])
            except Exception:
                score = None
        bbox = None
        if isinstance(boxes, (list, tuple)) and index < len(boxes):
            bbox = polygon_to_bbox(boxes[index])
        lines.append(OcrLineResult(text=str(text), confidence=score, bbox=bbox))
    return lines


def parse_old_style(items: list[Any]) -> list[OcrLineResult]:
    if len(items) == 1 and isinstance(items[0], list):
        items = items[0]
    lines: list[OcrLineResult] = []
    for entry in items:
        try:
            box, text_score = entry[0], entry[1]
            text = text_score[0]
            score = float(text_score[1]) if len(text_score) > 1 else None
            lines.append(OcrLineResult(text=str(text), confidence=score, bbox=polygon_to_bbox(box)))
        except Exception:
            continue
    return lines


def polygon_to_bbox(poly: Any) -> tuple[int, int, int, int] | None:
    try:
        arr = np.asarray(poly, dtype=np.float32).reshape(-1, 2)
        return int(arr[:, 0].min()), int(arr[:, 1].min()), int(arr[:, 0].max()), int(arr[:, 1].max())
    except Exception:
        return None
