# Board2Notes Backend

FastAPI backend for running the Board2Notes PyTorch checkpoints directly from `Models/`.

## Start

```powershell
cd Backend
$env:B2N_REQUIRE_CUDA="1"
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

The service uses these model paths by default:

- `../Models/TTM-V2/stage3_polish_final_best.pt`
- `../Models/YGM V3 Model B2N Nafnet/final_model.pt`

Override them with environment variables if needed:

```powershell
$env:B2N_MODEL1_PATH="C:\path\to\stage3_polish_final_best.pt"
$env:B2N_MODEL2_PATH="C:\path\to\final_model.pt"
```

## CUDA Setup

This backend is designed to run the PyTorch checkpoints on GPU when an NVIDIA GPU is available.
If `/health` reports `"device": "cpu"` or `torch` ends with `+cpu`, install CUDA PyTorch:

```powershell
python -m pip install --upgrade --index-url https://download.pytorch.org/whl/cu128 "torch==2.9.0+cu128" "torchvision==0.24.0+cu128"
```

Useful runtime flags:

```powershell
$env:B2N_DEVICE="cuda"
$env:B2N_REQUIRE_CUDA="1"
$env:B2N_USE_AMP="1"
$env:B2N_MODEL2_BATCH_SIZE="4"
```

Model 1 postprocess trims a small margin inside the detected board before sending the crop to Model 2.
This removes physical board edges and screws that can remain in the segmentation. The default is `0.025`.

```powershell
$env:B2N_BOARD_CONTENT_MARGIN_RATIO="0.025"
```

Use `0` to disable the inner crop, or try `0.015..0.04` depending on how much border remains.
The debug artifact `model1_outer_perspective_crop.png` keeps the full detected board, while
`model1_perspective_crop.png` is the cleaned content crop used by the pipeline.

Run a warmup request after startup to load weights and compile cuDNN choices before testing latency:

```powershell
Invoke-RestMethod -Method Post http://127.0.0.1:8000/api/v1/warmup
```

`B2N_CUDNN_BENCHMARK` defaults to `0` to avoid long first-request algorithm searches on Windows/WDDM. Set it to `1` only if you prefer maximum steady-state throughput and can tolerate slower cold starts.

## Smoke Test

```powershell
cd Backend
python scripts/smoke_models.py
```

## Main Endpoints

- `GET /health`
- `GET /api/v1/models`
- `POST /api/v1/model1/detect`
- `POST /api/v1/model2/enhance`
- `POST /api/v1/pipeline`
- `GET /api/v1/artifacts/{job_id}/{artifact_name}`

`/api/v1/pipeline` accepts `run_ocr=true|false`. For fast visual/model testing, keep it `false`; PaddleOCR is much slower than the PyTorch model passes on this Windows CPU setup.

Artifacts are saved under `Backend/outputs/{job_id}/`.
