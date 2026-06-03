from __future__ import annotations

import json
import mimetypes
import re
import uuid
from pathlib import Path
from typing import Any

import cv2
import numpy as np


class ArtifactStore:
    def __init__(self, root: Path):
        self.root = root
        self.root.mkdir(parents=True, exist_ok=True)

    def create_job(self) -> tuple[str, Path]:
        job_id = uuid.uuid4().hex
        job_dir = self.root / job_id
        job_dir.mkdir(parents=True, exist_ok=False)
        return job_id, job_dir

    def get_or_create_job(self, job_id: str) -> tuple[str, Path]:
        if not re.fullmatch(r"[A-Za-z0-9_\-.]+", job_id):
            raise FileNotFoundError(job_id)
        job_dir = (self.root / job_id).resolve()
        root = self.root.resolve()
        if root not in job_dir.parents and job_dir != root:
            raise FileNotFoundError(job_id)
        job_dir.mkdir(parents=True, exist_ok=True)
        return job_id, job_dir

    def url(self, job_id: str, file_name: str) -> str:
        return f"/api/v1/artifacts/{job_id}/{file_name}"

    def save_image(self, job_id: str, job_dir: Path, name: str, image: np.ndarray) -> str:
        file_name = f"{name}.png"
        path = job_dir / file_name
        image = normalize_image_for_write(image)
        if image.ndim == 2:
            ok = cv2.imwrite(str(path), image)
        else:
            ok = cv2.imwrite(str(path), cv2.cvtColor(image, cv2.COLOR_RGB2BGR))
        if not ok:
            raise RuntimeError(f"Could not write artifact: {path}")
        return self.url(job_id, file_name)

    def save_text(self, job_id: str, job_dir: Path, name: str, text: str) -> str:
        file_name = f"{name}.txt"
        path = job_dir / file_name
        path.write_text(text, encoding="utf-8")
        return self.url(job_id, file_name)

    def save_json(self, job_id: str, job_dir: Path, name: str, payload: dict[str, Any]) -> str:
        file_name = f"{name}.json"
        path = job_dir / file_name
        path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        return self.url(job_id, file_name)

    def resolve(self, job_id: str, artifact_name: str) -> tuple[Path, str]:
        if not re.fullmatch(r"[A-Za-z0-9_\-.]+", job_id):
            raise FileNotFoundError(job_id)
        if not re.fullmatch(r"[A-Za-z0-9_\-.]+", artifact_name):
            raise FileNotFoundError(artifact_name)
        job_dir = (self.root / job_id).resolve()
        root = self.root.resolve()
        if root not in job_dir.parents and job_dir != root:
            raise FileNotFoundError(job_id)

        candidates = [job_dir / artifact_name]
        if "." not in artifact_name:
            candidates.extend([job_dir / f"{artifact_name}.png", job_dir / f"{artifact_name}.txt", job_dir / f"{artifact_name}.json"])

        for candidate in candidates:
            path = candidate.resolve()
            if job_dir in path.parents and path.exists() and path.is_file():
                media_type = mimetypes.guess_type(str(path))[0] or "application/octet-stream"
                return path, media_type
        raise FileNotFoundError(artifact_name)


def normalize_image_for_write(image: np.ndarray) -> np.ndarray:
    if image.dtype == np.uint8:
        return image
    return np.clip(image, 0, 255).round().astype(np.uint8)
