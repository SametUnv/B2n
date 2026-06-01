from __future__ import annotations

from dataclasses import dataclass
from math import hypot

import cv2
import numpy as np


@dataclass(frozen=True)
class LetterboxMeta:
    original_width: int
    original_height: int
    resized_width: int
    resized_height: int
    pad_x: int
    pad_y: int
    scale: float


@dataclass(frozen=True)
class QuadDetection:
    quad: np.ndarray
    bbox: tuple[int, int, int, int]
    confidence: float
    strategy: str


def letterbox_rgb(image_rgb: np.ndarray, size: int = 640, pad_value: int = 114) -> tuple[np.ndarray, LetterboxMeta]:
    height, width = image_rgb.shape[:2]
    scale = min(size / width, size / height)
    resized_width = max(1, int(round(width * scale)))
    resized_height = max(1, int(round(height * scale)))
    pad_x = (size - resized_width) // 2
    pad_y = (size - resized_height) // 2

    canvas = np.full((size, size, 3), pad_value, dtype=np.uint8)
    resized = cv2.resize(image_rgb, (resized_width, resized_height), interpolation=cv2.INTER_AREA)
    canvas[pad_y : pad_y + resized_height, pad_x : pad_x + resized_width] = resized
    meta = LetterboxMeta(width, height, resized_width, resized_height, pad_x, pad_y, scale)
    return canvas, meta


def unletterbox_probability(prob_640: np.ndarray, meta: LetterboxMeta) -> np.ndarray:
    y0 = meta.pad_y
    x0 = meta.pad_x
    unpadded = prob_640[y0 : y0 + meta.resized_height, x0 : x0 + meta.resized_width]
    return cv2.resize(unpadded, (meta.original_width, meta.original_height), interpolation=cv2.INTER_LINEAR)


def clean_mask(probability: np.ndarray, threshold: float) -> np.ndarray:
    binary = (probability >= threshold).astype(np.uint8)
    min_side = min(binary.shape[:2])
    kernel_size = int(np.clip(round(min_side / 450), 3, 9))
    if kernel_size % 2 == 0:
        kernel_size += 1
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (kernel_size, kernel_size))
    opened = cv2.morphologyEx(binary, cv2.MORPH_OPEN, kernel, iterations=1)
    closed = cv2.morphologyEx(opened, cv2.MORPH_CLOSE, kernel, iterations=2)
    filled = fill_holes(closed)
    return largest_component(filled)


def fill_holes(mask: np.ndarray) -> np.ndarray:
    h, w = mask.shape[:2]
    flood = (mask * 255).astype(np.uint8)
    flood_mask = np.zeros((h + 2, w + 2), dtype=np.uint8)
    cv2.floodFill(flood, flood_mask, (0, 0), 255)
    holes = cv2.bitwise_not(flood)
    return np.where((mask > 0) | (holes > 0), 1, 0).astype(np.uint8)


def largest_component(mask: np.ndarray) -> np.ndarray:
    count, labels, stats, _ = cv2.connectedComponentsWithStats(mask.astype(np.uint8), connectivity=8)
    if count <= 1:
        return mask.astype(np.uint8)
    areas = stats[1:, cv2.CC_STAT_AREA]
    best_label = int(np.argmax(areas)) + 1
    return (labels == best_label).astype(np.uint8)


def bounding_box(mask: np.ndarray) -> tuple[int, int, int, int]:
    ys, xs = np.where(mask > 0)
    if len(xs) == 0 or len(ys) == 0:
        return 0, 0, 0, 0
    return int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1


def detect_quad(mask: np.ndarray) -> QuadDetection:
    height, width = mask.shape[:2]
    bbox = bounding_box(mask)
    if bbox[2] <= bbox[0] or bbox[3] <= bbox[1]:
        return QuadDetection(_bbox_quad(center_box(width, height)), center_box(width, height), 0.0, "center_fallback")

    contours, _ = cv2.findContours((mask * 255).astype(np.uint8), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contours:
        return QuadDetection(_bbox_quad(bbox), bbox, 0.2, "bbox_fallback")

    contour = max(contours, key=cv2.contourArea)
    perimeter = cv2.arcLength(contour, True)
    for ratio in (0.01, 0.015, 0.02, 0.03, 0.04, 0.06, 0.08):
        approx = cv2.approxPolyDP(contour, ratio * perimeter, True)
        if len(approx) == 4:
            quad = order_points(approx.reshape(4, 2).astype(np.float32))
            if sane_quad(quad, width, height):
                return QuadDetection(expand_quad(quad, width, height), bbox, 0.92, "contour_approx")

    hull = cv2.convexHull(contour).reshape(-1, 2).astype(np.float32)
    if len(hull) >= 4:
        quad = order_points(np.array([
            hull[np.argmin(hull[:, 0] + hull[:, 1])],
            hull[np.argmax(hull[:, 0] - hull[:, 1])],
            hull[np.argmax(hull[:, 0] + hull[:, 1])],
            hull[np.argmin(hull[:, 0] - hull[:, 1])],
        ], dtype=np.float32))
        if sane_quad(quad, width, height):
            return QuadDetection(expand_quad(quad, width, height), bbox, 0.84, "convex_hull_extremes")

    rect = cv2.minAreaRect(contour)
    quad = order_points(cv2.boxPoints(rect).astype(np.float32))
    if sane_quad(quad, width, height):
        return QuadDetection(expand_quad(quad, width, height), bbox, 0.65, "min_area_rect_fallback")

    return QuadDetection(_bbox_quad(bbox), bbox, 0.35, "bbox_fallback")


def center_box(width: int, height: int) -> tuple[int, int, int, int]:
    horizontal_inset = int(width * 0.08)
    vertical_inset = int(height * 0.18)
    return horizontal_inset, vertical_inset, width - horizontal_inset, height - vertical_inset


def _bbox_quad(bbox: tuple[int, int, int, int]) -> np.ndarray:
    left, top, right, bottom = bbox
    return np.array([[left, top], [right, top], [right, bottom], [left, bottom]], dtype=np.float32)


def order_points(points: np.ndarray) -> np.ndarray:
    points = points.astype(np.float32)
    ordered = np.zeros((4, 2), dtype=np.float32)
    sums = points.sum(axis=1)
    diffs = np.diff(points, axis=1).reshape(-1)
    ordered[0] = points[np.argmin(sums)]
    ordered[2] = points[np.argmax(sums)]
    ordered[1] = points[np.argmin(diffs)]
    ordered[3] = points[np.argmax(diffs)]
    return ordered


def expand_quad(quad: np.ndarray, width: int, height: int, margin_ratio: float = 0.018) -> np.ndarray:
    center = quad.mean(axis=0, keepdims=True)
    expanded = center + (quad - center) * (1.0 + margin_ratio)
    expanded[:, 0] = np.clip(expanded[:, 0], 0, width - 1)
    expanded[:, 1] = np.clip(expanded[:, 1], 0, height - 1)
    return expanded.astype(np.float32)


def inset_quad(quad: np.ndarray, margin_ratio: float) -> np.ndarray:
    margin_ratio = float(np.clip(margin_ratio, 0.0, 0.12))
    if margin_ratio <= 0:
        return quad.astype(np.float32)
    center = quad.mean(axis=0, keepdims=True)
    inset = center + (quad - center) * (1.0 - margin_ratio)
    return inset.astype(np.float32)


def sane_quad(quad: np.ndarray, width: int, height: int) -> bool:
    area = abs(cv2.contourArea(quad.reshape(-1, 1, 2)))
    if area < width * height * 0.003:
        return False
    sides = [
        _distance(quad[0], quad[1]),
        _distance(quad[1], quad[2]),
        _distance(quad[2], quad[3]),
        _distance(quad[3], quad[0]),
    ]
    return min(sides) >= 8


def warp_perspective(image_rgb: np.ndarray, quad: np.ndarray, max_output_side: int = 2400) -> np.ndarray:
    quad = order_points(quad)
    raw_width = max(_distance(quad[0], quad[1]), _distance(quad[3], quad[2]))
    raw_height = max(_distance(quad[0], quad[3]), _distance(quad[1], quad[2]))
    if raw_width <= 1 or raw_height <= 1:
        raise ValueError("Invalid quadrilateral for perspective correction.")
    scale = min(1.0, max_output_side / max(raw_width, raw_height))
    out_width = max(1, int(round(raw_width * scale)))
    out_height = max(1, int(round(raw_height * scale)))
    dst = np.array(
        [[0, 0], [out_width - 1, 0], [out_width - 1, out_height - 1], [0, out_height - 1]],
        dtype=np.float32,
    )
    matrix = cv2.getPerspectiveTransform(quad.astype(np.float32), dst)
    return cv2.warpPerspective(
        image_rgb,
        matrix,
        (out_width, out_height),
        flags=cv2.INTER_CUBIC,
        borderMode=cv2.BORDER_REPLICATE,
    )


def crop_bbox(image_rgb: np.ndarray, bbox: tuple[int, int, int, int]) -> np.ndarray:
    left, top, right, bottom = bbox
    left = max(0, left)
    top = max(0, top)
    right = min(image_rgb.shape[1], right)
    bottom = min(image_rgb.shape[0], bottom)
    if right <= left or bottom <= top:
        return image_rgb.copy()
    return image_rgb[top:bottom, left:right].copy()


def mask_to_gray(mask: np.ndarray) -> np.ndarray:
    return (mask.astype(np.uint8) * 255)


def probability_to_gray(probability: np.ndarray) -> np.ndarray:
    return np.clip(probability * 255.0, 0, 255).astype(np.uint8)


def overlay_mask(image_rgb: np.ndarray, mask: np.ndarray, alpha: float = 0.42) -> np.ndarray:
    overlay = image_rgb.copy().astype(np.float32)
    color = np.zeros_like(overlay)
    color[..., 0] = 40
    color[..., 1] = 210
    color[..., 2] = 120
    m = mask.astype(bool)
    overlay[m] = overlay[m] * (1.0 - alpha) + color[m] * alpha
    return np.clip(overlay, 0, 255).astype(np.uint8)


def draw_quad(image_rgb: np.ndarray, quad: np.ndarray) -> np.ndarray:
    out = image_rgb.copy()
    pts = quad.round().astype(np.int32).reshape((-1, 1, 2))
    cv2.polylines(out, [pts], isClosed=True, color=(255, 80, 30), thickness=max(2, image_rgb.shape[1] // 450))
    for x, y in pts.reshape(-1, 2):
        cv2.circle(out, (int(x), int(y)), radius=max(4, image_rgb.shape[1] // 350), color=(30, 120, 255), thickness=-1)
    return out


def _distance(a: np.ndarray, b: np.ndarray) -> float:
    return float(hypot(float(a[0] - b[0]), float(a[1] - b[1])))
