from __future__ import annotations

import torch
import torch.nn as nn
import torch.nn.functional as F


class LayerNorm2d(nn.Module):
    def __init__(self, channels: int, eps: float = 1e-6):
        super().__init__()
        self.weight = nn.Parameter(torch.ones(channels))
        self.bias = nn.Parameter(torch.zeros(channels))
        self.eps = eps

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        mean = x.mean(1, keepdim=True)
        variance = (x - mean).pow(2).mean(1, keepdim=True)
        x = (x - mean) / torch.sqrt(variance + self.eps)
        return x * self.weight[None, :, None, None] + self.bias[None, :, None, None]


class SimpleGate(nn.Module):
    def forward(self, x: torch.Tensor) -> torch.Tensor:
        x1, x2 = x.chunk(2, dim=1)
        return x1 * x2


class NAFBlock(nn.Module):
    def __init__(self, channels: int, dw_expand: int = 2, ffn_expand: int = 2):
        super().__init__()
        dw_channels = channels * dw_expand
        self.conv1 = nn.Conv2d(channels, dw_channels, 1)
        self.conv2 = nn.Conv2d(dw_channels, dw_channels, 3, 1, 1, groups=dw_channels)
        self.conv3 = nn.Conv2d(dw_channels // 2, channels, 1)
        self.sca = nn.Sequential(
            nn.AdaptiveAvgPool2d(1),
            nn.Conv2d(dw_channels // 2, dw_channels // 2, 1),
        )
        self.sg = SimpleGate()

        ffn_channels = channels * ffn_expand
        self.conv4 = nn.Conv2d(channels, ffn_channels, 1)
        self.conv5 = nn.Conv2d(ffn_channels // 2, channels, 1)

        self.norm1 = LayerNorm2d(channels)
        self.norm2 = LayerNorm2d(channels)
        self.beta = nn.Parameter(torch.zeros((1, channels, 1, 1)))
        self.gamma = nn.Parameter(torch.zeros((1, channels, 1, 1)))

    def forward(self, inp: torch.Tensor) -> torch.Tensor:
        x = self.norm1(inp)
        x = self.conv1(x)
        x = self.conv2(x)
        x = self.sg(x)
        x = x * self.sca(x)
        x = self.conv3(x)
        y = inp + x * self.beta

        x = self.norm2(y)
        x = self.conv4(x)
        x = self.sg(x)
        x = self.conv5(x)
        return y + x * self.gamma


class NAFNetMultiHead(nn.Module):
    def __init__(
        self,
        img_channel: int = 3,
        width: int = 64,
        middle_blk_num: int = 12,
        enc_blk_nums: tuple[int, ...] = (2, 2, 4, 8),
        dec_blk_nums: tuple[int, ...] = (2, 2, 2, 2),
    ):
        super().__init__()
        self.intro = nn.Conv2d(img_channel, width, 3, 1, 1)
        self.encoders = nn.ModuleList()
        self.decoders = nn.ModuleList()
        self.ups = nn.ModuleList()
        self.downs = nn.ModuleList()

        channels = width
        for block_count in enc_blk_nums:
            self.encoders.append(nn.Sequential(*[NAFBlock(channels) for _ in range(block_count)]))
            self.downs.append(nn.Conv2d(channels, 2 * channels, 2, 2))
            channels *= 2

        self.middle_blks = nn.Sequential(*[NAFBlock(channels) for _ in range(middle_blk_num)])

        for block_count in dec_blk_nums:
            self.ups.append(nn.Sequential(nn.Conv2d(channels, channels * 2, 1, bias=False), nn.PixelShuffle(2)))
            channels //= 2
            self.decoders.append(nn.Sequential(*[NAFBlock(channels) for _ in range(block_count)]))

        self.ending_restored = nn.Conv2d(width, 3, 3, 1, 1)
        self.ending_ocr = nn.Conv2d(width, 3, 3, 1, 1)
        self.mask_head = nn.Sequential(
            nn.Conv2d(width, width // 2, 3, 1, 1),
            nn.GELU(),
            nn.Conv2d(width // 2, 1, 3, 1, 1),
        )
        self.padder_size = 2 ** len(self.encoders)

    def check_image_size(self, x: torch.Tensor) -> torch.Tensor:
        _, _, height, width = x.size()
        pad_h = (self.padder_size - height % self.padder_size) % self.padder_size
        pad_w = (self.padder_size - width % self.padder_size) % self.padder_size
        return F.pad(x, (0, pad_w, 0, pad_h))

    def forward_features(self, inp: torch.Tensor) -> tuple[torch.Tensor, torch.Tensor, int, int]:
        _, _, height, width = inp.shape
        x_in = self.check_image_size(inp)
        x = self.intro(x_in)
        skips = []
        for encoder, down in zip(self.encoders, self.downs):
            x = encoder(x)
            skips.append(x)
            x = down(x)

        x = self.middle_blks(x)

        for decoder, up, skip in zip(self.decoders, self.ups, skips[::-1]):
            x = up(x)
            x = x + skip
            x = decoder(x)

        return x, x_in, height, width

    def forward(self, inp: torch.Tensor) -> dict[str, torch.Tensor]:
        feat, x_in, height, width = self.forward_features(inp)
        return {
            "restored": (self.ending_restored(feat) + x_in)[:, :, :height, :width],
            "ocr": (self.ending_ocr(feat) + x_in)[:, :, :height, :width],
            "mask_logits": self.mask_head(feat)[:, :, :height, :width],
        }
