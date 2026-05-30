# Board2Notes Backend API Integration Guide

Bu dokuman Android/Kotlin frontend'i daha sonra FastAPI backend'e baglamak icin eklendi. Mevcut Android koduna bu is kapsaminda dokunulmadi.

## Backend'i Baslatma

```powershell
cd Backend
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Android emulator icin base URL:

```text
http://10.0.2.2:8000
```

Fiziksel cihaz icin bilgisayarin LAN IP adresini kullanin:

```text
http://192.168.x.x:8000
```

## Mevcut Kotlin Akisinin API Karsiligi

- `BoardSegmentationEngine.detectBoard(...)`
  - Backend: `POST /api/v1/model1/detect`
  - Multipart field: `image`
  - Form field: `threshold`
  - Donen ana alanlar: `bbox`, `quad`, `confidence`, `strategy`, `warnings`, `artifacts.mask`, `artifacts.overlay`, `artifacts.perspective_crop`

- `BoardEnhancementEngine.enhance(...)`
  - Backend: `POST /api/v1/model2/enhance`
  - Multipart field: `image`
  - Donen ana alanlar: `artifacts.restored`, `artifacts.ocr_enhanced`, `artifacts.text_mask`, `artifacts.white_canvas`

- Tam akisi tek istekte calistirmak
  - Backend: `POST /api/v1/pipeline`
  - Siralama: Model 1 -> perspektif crop -> Model 2 -> OCR -> note formatting
  - Android tarafinda ilk entegrasyon icin en hizli yol budur.
  - Form field `run_ocr=false` gonderilirse OCR atlanir; model gorselleri ve not canvas daha hizli uretilir.

## Multipart Ornegi

```kotlin
val imageBody = imageFile.asRequestBody("image/jpeg".toMediaType())
val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, imageBody)
val threshold = "0.50".toRequestBody("text/plain".toMediaType())
val runOcr = "false".toRequestBody("text/plain".toMediaType())
```

Retrofit servis taslagi:

```kotlin
interface Board2NotesBackendApi {
    @Multipart
    @POST("/api/v1/pipeline")
    suspend fun runPipeline(
        @Part image: MultipartBody.Part,
        @Part("threshold") threshold: RequestBody,
        @Part("run_ocr") runOcr: RequestBody
    ): PipelineResponseDto

    @Multipart
    @POST("/api/v1/model1/detect")
    suspend fun detectBoard(
        @Part image: MultipartBody.Part,
        @Part("threshold") threshold: RequestBody
    ): Model1ResponseDto

    @Multipart
    @POST("/api/v1/model2/enhance")
    suspend fun enhanceBoard(
        @Part image: MultipartBody.Part
    ): Model2ResponseDto
}
```

## Response Sekli

`POST /api/v1/pipeline` kisaltilmis ornek:

```json
{
  "job_id": "9e2f...",
  "model1": {
    "bbox": {"left": 120, "top": 80, "right": 1800, "bottom": 920},
    "quad": {
      "top_left": {"x": 130.0, "y": 86.0},
      "top_right": {"x": 1780.0, "y": 90.0},
      "bottom_right": {"x": 1810.0, "y": 910.0},
      "bottom_left": {"x": 118.0, "y": 900.0}
    },
    "artifacts": {
      "overlay": "/api/v1/artifacts/{job_id}/model1_overlay.png",
      "perspective_crop": "/api/v1/artifacts/{job_id}/model1_perspective_crop.png"
    }
  },
  "model2": {
    "artifacts": {
      "ocr_enhanced": "/api/v1/artifacts/{job_id}/model2_ocr_enhanced.png",
      "white_canvas": "/api/v1/artifacts/{job_id}/model2_white_canvas.png"
    }
  },
  "ocr_text": "...",
  "note": {
    "title": "...",
    "body": "..."
  }
}
```

Artifact URL'leri relative doner. Android tarafinda tam URL su sekilde kurulabilir:

```kotlin
val fullArtifactUrl = backendBaseUrl.trimEnd('/') + relativeArtifactUrl
```

## Android Tarafinda Onerilen Entegrasyon Sirasi

1. ONNX engine'leri silmeden yeni bir `RemoteBoard2NotesPipeline` adapter'i ekleyin.
2. Ayarlara `Local ONNX` / `Backend API` gibi bir runtime secimi koyun.
3. Ilk iterasyonda sadece `POST /api/v1/pipeline` kullanin.
4. Mevcut ekran state'lerine backend artifact gorsellerini indirip `Bitmap` olarak verin:
   - `model1_overlay.png` -> BoardDetection preview
   - `model1_perspective_crop.png` -> crop
   - `model2_ocr_enhanced.png` -> OCR goruntusu
   - `model2_white_canvas.png` -> beyaz sayfa
5. Daha sonra istenirse mevcut adim adim UI akisini korumak icin `/model1/detect` ve `/model2/enhance` endpointlerine bolun.

## Notlar

- Backend PyTorch `.pt` checkpointlerini kullanir; Android APK icine buyuk model dosyasi koymaya gerek kalmaz.
- PaddleOCR backend tarafinda tembel yuklenir. Ilk OCR istegi sonraki isteklerden daha yavas olabilir.
- Emulator disindaki cihazlarda bilgisayar ve telefon ayni agda olmali, Windows firewall `8000` portuna izin vermelidir.
