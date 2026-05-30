from __future__ import annotations

import cv2
import numpy as np
from fastapi import UploadFile


async def read_upload_rgb(file: UploadFile) -> np.ndarray:
    data = await file.read()
    if not data:
        raise ValueError("Uploaded image is empty.")
    buffer = np.frombuffer(data, dtype=np.uint8)
    bgr = cv2.imdecode(buffer, cv2.IMREAD_COLOR)
    if bgr is None:
        raise ValueError("Uploaded file could not be decoded as an image.")
    return cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)


def ensure_uint8_rgb(image: np.ndarray) -> np.ndarray:
    if image.ndim != 3 or image.shape[2] != 3:
        raise ValueError("Expected RGB image with 3 channels.")
    if image.dtype == np.uint8:
        return image
    return np.clip(image, 0, 255).astype(np.uint8)
