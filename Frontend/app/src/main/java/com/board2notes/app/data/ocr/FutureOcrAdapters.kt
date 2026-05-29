package com.board2notes.app.data.ocr

import android.graphics.Bitmap
import com.board2notes.app.domain.engine.OcrEngine
import com.board2notes.app.domain.model.OcrResult

class TrocrOcrEngine : OcrEngine {
    override suspend fun recognize(image: Bitmap): OcrResult {
        error("TrOCR modeli araştırma slotudur; tokenizer/config/model dosyaları repoda yok.")
    }
}

class PaddleOcrEngine : OcrEngine {
    override suspend fun recognize(image: Bitmap): OcrResult {
        error("PaddleOCR mobile modeli benchmark slotudur; runtime dosyaları repoda yok.")
    }
}
