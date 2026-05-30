from __future__ import annotations

import sys
from pathlib import Path

import torch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from app.config import load_settings  # noqa: E402
from app.services.model1_segmentation import Model1SegmentationService  # noqa: E402
from app.services.model2_enhancement import Model2EnhancementService  # noqa: E402


def main() -> None:
    settings = load_settings()
    print(f"device={settings.device}")

    model1 = Model1SegmentationService(settings)._load_model()
    with torch.inference_mode():
        out1 = model1(torch.randn(1, 3, 64, 64, device=settings.device))
        tensor1 = out1["out"] if isinstance(out1, dict) else out1
    print(f"model1_shape={tuple(tensor1.shape)}")

    model2 = Model2EnhancementService(settings)._load_model()
    with torch.inference_mode():
        out2 = model2(torch.randn(1, 3, 64, 64, device=settings.device))
    print(f"model2_shapes={{{', '.join(f'{k}: {tuple(v.shape)}' for k, v in out2.items())}}}")


if __name__ == "__main__":
    main()
