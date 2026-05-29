package com.board2notes.app.data.ml

import android.graphics.Bitmap
import com.board2notes.app.domain.engine.BoardSegmentationEngine
import com.board2notes.app.domain.model.BoardDetectionResult

class SafeBoardSegmentationEngine(
    private val primary: BoardSegmentationEngine,
    private val fallback: BoardSegmentationEngine = FallbackBoardSegmentationEngine()
) : BoardSegmentationEngine {
    override suspend fun detectBoard(bitmap: Bitmap, threshold: Float): BoardDetectionResult =
        try {
            primary.detectBoard(bitmap, threshold)
        } catch (error: Throwable) {
            val fallbackResult = fallback.detectBoard(bitmap, threshold)
            fallbackResult.copy(
                warnings = fallbackResult.warnings + "Model 1 hatası: ${error.message ?: error::class.java.simpleName}"
            )
        }
}
