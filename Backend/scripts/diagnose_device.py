from __future__ import annotations

import torch


def main() -> None:
    print(f"torch={torch.__version__}")
    print(f"torch_cuda={torch.version.cuda}")
    print(f"cuda_available={torch.cuda.is_available()}")
    print(f"device_count={torch.cuda.device_count()}")
    if torch.cuda.is_available():
        for index in range(torch.cuda.device_count()):
            props = torch.cuda.get_device_properties(index)
            total_mb = round(props.total_memory / 1024 / 1024)
            print(f"gpu[{index}]={torch.cuda.get_device_name(index)} memory_mb={total_mb}")


if __name__ == "__main__":
    main()
