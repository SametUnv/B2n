package com.board2notes.app.data.ocr

import android.graphics.Bitmap
import com.board2notes.app.domain.engine.OcrEngine
import com.board2notes.app.domain.model.OcrResult

class SafeOcrEngine(
    private val primary: OcrEngine,
    private val fallback: OcrEngine = MockOcrEngine()
) : OcrEngine {
    override suspend fun recognize(image: Bitmap): OcrResult =
        try {
            primary.recognize(image)
        } catch (error: Throwable) {
            val fallbackResult = fallback.recognize(image)
            fallbackResult.copy(
                warnings = fallbackResult.warnings + "ML Kit OCR hatası: ${error.message ?: error::class.java.simpleName}"
            )
        }
}
