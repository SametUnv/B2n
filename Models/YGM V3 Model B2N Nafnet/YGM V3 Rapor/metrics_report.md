# Board2Notes Model 2 — Başarı Raporu

Final stage: **stage3_gan** | Final score: **0.29822**

## Test Seti Metrikleri (stage ilerlemesi)

| Metrik | Baseline | Stage1 | Stage2 | Stage3-GAN |
|---|---|---|---|---|
| OCR PSNR ↑ | 15.3218 | 16.9724 | 18.6394 | 24.8144 |
| OCR SSIM ↑ | 0.3583 | 0.1401 | 0.1765 | 0.5082 |
| OCR MS-SSIM ↑ | 0.6839 | 0.4262 | 0.4339 | 0.7888 |
| Mask IoU ↑ | 0.0000 | 0.5735 | 0.5682 | 0.5778 |
| Mask Dice ↑ | 0.0000 | 0.7124 | 0.7087 | 0.7159 |
| Mask F1 ↑ | 0.0000 | 0.7124 | 0.7087 | 0.7159 |
| Mask Boundary-F1 ↑ | 0.0000 | 0.9607 | 0.9656 | 0.9712 |
| Canvas PSNR ↑ | 14.0819 | 30.8829 | 30.3231 | 30.8369 |
| Canvas SSIM ↑ | 0.6113 | 0.9775 | 0.9765 | 0.9767 |
| Text Contrast ↑ | 0.2268 | 0.3660 | 0.3823 | 0.3887 |
| Sharpness ↑ | 0.0200 | 0.1069 | 0.0488 | 0.0148 |

## Baseline -> Final İyileşme

| Metrik | Baseline | Final | Değişim |
|---|---|---|---|
| OCR PSNR ↑ | 15.3218 | 24.8144 | +62.0% |
| OCR SSIM ↑ | 0.3583 | 0.5082 | +41.8% |
| Mask IoU ↑ | 0.0000 | 0.5778 | +49115186283.0% |
| Mask F1 ↑ | 0.0000 | 0.7159 | +52925022372.3% |
| Canvas PSNR ↑ | 14.0819 | 30.8369 | +119.0% |
| Canvas SSIM ↑ | 0.6113 | 0.9767 | +59.8% |