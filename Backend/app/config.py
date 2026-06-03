from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

import torch


@dataclass(frozen=True)
class Settings:
    backend_dir: Path
    repo_root: Path
    outputs_dir: Path
    model1_path: Path
    model2_path: Path
    device: str
    require_cuda: bool
    use_amp: bool
    model1_image_size: int = 640
    model1_pad_value: int = 114
    model2_tile_size: int = 512
    model2_overlap: int = 64
    model2_batch_size: int = 1
    model2_max_input_side: int = 1536
    model2_ink_darkness: float = 0.55
    perspective_max_output_side: int = 2400
    board_content_margin_ratio: float = 0.025
    ocr_lang: str = "tr"
    # --- LM Studio (Qwen 2.5 VL) OCR ---
    lmstudio_base_url: str = "http://127.0.0.1:1234/v1"
    lmstudio_ocr_model: str = "qwen2.5-vl-3b-instruct"
    lmstudio_timeout_s: float = 120.0
    # Gorsel detayini korumak icin yuksek tutulur; yalnizca en uzun kenar bunu asarsa
    # kuculturuz. Model baglam tasarsa (HTTP 400) servis otomatik olarak kademeli daha
    # kucuk cozunurlukle tekrar dener. B2N_LMSTUDIO_IMAGE_MAX_SIDE ile ayarlanir.
    lmstudio_image_max_side: int = 2048
    lmstudio_max_tokens: int = 4096
    # --- Google Generative Language API (Gemma) "anlat" ---
    gemini_base_url: str = "https://generativelanguage.googleapis.com/v1beta"
    gemini_model: str = "gemma-4-31b-it"
    gemini_api_key: str = "AQ.Ab8RN6I6GPqLuLtfcAuiKjYjKS7E7hA73se5lsWoKaIK-yVt9w"
    gemini_timeout_s: float = 90.0


def load_settings() -> Settings:
    backend_dir = Path(__file__).resolve().parents[1]
    repo_root = backend_dir.parent
    outputs_dir = Path(os.getenv("B2N_OUTPUTS_DIR", backend_dir / "outputs")).resolve()
    model1_path = Path(
        os.getenv("B2N_MODEL1_PATH", repo_root / "Models" / "TTM-V2" / "stage3_polish_final_best.pt")
    ).resolve()
    model2_path = Path(
        os.getenv("B2N_MODEL2_PATH", repo_root / "Models" / "YGM V3 Model B2N Nafnet" / "final_model.pt")
    ).resolve()
    requested_device = os.getenv("B2N_DEVICE", "cuda" if torch.cuda.is_available() else "cpu")
    require_cuda = os.getenv("B2N_REQUIRE_CUDA", "0").strip().lower() in {"1", "true", "yes", "on"}
    if requested_device.startswith("cuda") and not torch.cuda.is_available():
        if require_cuda:
            raise RuntimeError(
                "B2N_REQUIRE_CUDA=1 but this Python environment cannot use CUDA. "
                "Install a CUDA-enabled PyTorch wheel."
            )
        requested_device = "cpu"
    device = requested_device
    use_amp = os.getenv("B2N_USE_AMP", "1").strip().lower() in {"1", "true", "yes", "on"}
    use_amp = use_amp and device.startswith("cuda")
    configure_torch_runtime(device)

    return Settings(
        backend_dir=backend_dir,
        repo_root=repo_root,
        outputs_dir=outputs_dir,
        model1_path=model1_path,
        model2_path=model2_path,
        device=device,
        require_cuda=require_cuda,
        use_amp=use_amp,
        model2_batch_size=int(os.getenv("B2N_MODEL2_BATCH_SIZE", "4" if device.startswith("cuda") else "1")),
        model2_max_input_side=int(os.getenv("B2N_MODEL2_MAX_INPUT_SIDE", "1536")),
        model2_ink_darkness=float(os.getenv("B2N_INK_DARKNESS", "0.55")),
        board_content_margin_ratio=float(os.getenv("B2N_BOARD_CONTENT_MARGIN_RATIO", "0.025")),
        ocr_lang=os.getenv("B2N_OCR_LANG", "tr"),
        lmstudio_base_url=os.getenv("B2N_LMSTUDIO_URL", "http://127.0.0.1:1234/v1").strip().rstrip("/"),
        lmstudio_ocr_model=os.getenv("B2N_LMSTUDIO_OCR_MODEL", "qwen2.5-vl-3b-instruct"),
        lmstudio_timeout_s=float(os.getenv("B2N_LMSTUDIO_TIMEOUT_S", "120")),
        lmstudio_image_max_side=int(os.getenv("B2N_LMSTUDIO_IMAGE_MAX_SIDE", "2048")),
        lmstudio_max_tokens=int(os.getenv("B2N_LMSTUDIO_MAX_TOKENS", "4096")),
        gemini_base_url=os.getenv("B2N_GEMINI_URL", "https://generativelanguage.googleapis.com/v1beta").strip().rstrip("/"),
        gemini_model=os.getenv("B2N_GEMINI_MODEL", "gemma-4-31b-it"),
        gemini_api_key=os.getenv("B2N_GEMINI_API_KEY", "AQ.Ab8RN6I6GPqLuLtfcAuiKjYjKS7E7hA73se5lsWoKaIK-yVt9w"),
        gemini_timeout_s=float(os.getenv("B2N_GEMINI_TIMEOUT_S", "90")),
    )


def configure_torch_runtime(device: str) -> None:
    if not device.startswith("cuda"):
        torch.set_num_threads(max(1, int(os.getenv("B2N_CPU_THREADS", str(torch.get_num_threads())))))
        return
    benchmark = os.getenv("B2N_CUDNN_BENCHMARK", "0").strip().lower() in {"1", "true", "yes", "on"}
    torch.backends.cudnn.benchmark = benchmark
    torch.backends.cuda.matmul.allow_tf32 = True
    torch.backends.cudnn.allow_tf32 = True
    try:
        torch.set_float32_matmul_precision("high")
    except Exception:
        pass


def torch_runtime_info(device: str) -> dict:
    info = {
        "torch_version": torch.__version__,
        "torch_cuda_version": torch.version.cuda,
        "cuda_available": torch.cuda.is_available(),
        "selected_device": device,
        "cudnn_benchmark": torch.backends.cudnn.benchmark,
    }
    if torch.cuda.is_available():
        index = torch.device(device).index or 0 if device.startswith("cuda") else 0
        props = torch.cuda.get_device_properties(index)
        info.update(
            {
                "gpu_name": torch.cuda.get_device_name(index),
                "gpu_total_memory_mb": round(props.total_memory / 1024 / 1024),
                "gpu_allocated_mb": round(torch.cuda.memory_allocated(index) / 1024 / 1024, 1),
                "gpu_reserved_mb": round(torch.cuda.memory_reserved(index) / 1024 / 1024, 1),
            }
        )
    return info
