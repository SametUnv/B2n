package com.board2notes.app.data.ocr

import android.graphics.Bitmap
import com.board2notes.app.domain.engine.OcrEngine
import com.board2notes.app.domain.model.OcrLine
import com.board2notes.app.domain.model.OcrResult

class MockOcrEngine : OcrEngine {
    override suspend fun recognize(image: Bitmap): OcrResult {
        val text = "Board2Notes OCR hazır.\nML Kit çalıştırılamazsa bu geçici metin gösterilir."
        return OcrResult(
            rawText = text,
            lines = text.lines().map { OcrLine(it, null, null, emptyList()) },
            words = emptyList(),
            confidence = null,
            elapsedMs = 0L,
            warnings = listOf("Mock OCR engine kullanıldı.")
        )
    }
}
