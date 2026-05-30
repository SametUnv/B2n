$ErrorActionPreference = "Stop"

python -m pip install --upgrade --index-url https://download.pytorch.org/whl/cu128 "torch==2.9.0+cu128" "torchvision==0.24.0+cu128"

@'
import torch
print("torch", torch.__version__)
print("cuda_available", torch.cuda.is_available())
print("cuda_version", torch.version.cuda)
print("gpu", torch.cuda.get_device_name(0) if torch.cuda.is_available() else None)
'@ | python -
