package com.board2notes.app.domain.engine

import android.graphics.Bitmap
import com.board2notes.app.domain.model.BoardDetectionResult
import com.board2notes.app.domain.model.EnhancementMode
import com.board2notes.app.domain.model.EnhancementResult
import com.board2notes.app.domain.model.OcrResult

interface BoardSegmentationEngine {
    suspend fun detectBoard(bitmap: Bitmap, threshold: Float): BoardDetectionResult
}

interface BoardEnhancementEngine {
    suspend fun enhance(bitmap: Bitmap, mode: EnhancementMode): EnhancementResult
}

interface OcrEngine {
    suspend fun recognize(image: Bitmap): OcrResult
}
