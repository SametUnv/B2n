package com.board2notes.app.domain.pipeline

import android.graphics.Bitmap
import com.board2notes.app.domain.model.BoardDetectionResult
import com.board2notes.app.domain.model.DebugArtifact
import com.board2notes.app.domain.model.EnhancementResult
import com.board2notes.app.domain.model.FormattedNote
import com.board2notes.app.domain.model.OcrResult

sealed interface PipelineState {
    data object Idle : PipelineState
    data class ImageSelected(val image: Bitmap) : PipelineState
    data object DetectingBoard : PipelineState
    data class BoardDetected(val result: BoardDetectionResult) : PipelineState
    data object EnhancingImage : PipelineState
    data class Enhanced(val result: EnhancementResult) : PipelineState
    data object RunningOcr : PipelineState
    data class OcrCompleted(val result: OcrResult) : PipelineState
    data object FormattingNote : PipelineState
    data class Completed(val result: PipelineResult) : PipelineState
    data class Error(val message: String, val cause: Throwable? = null) : PipelineState
}

data class PipelineResult(
    val originalImage: Bitmap?,
    val boardDetection: BoardDetectionResult?,
    val enhancement: EnhancementResult?,
    val ocr: OcrResult?,
    val note: FormattedNote?,
    val debugArtifacts: List<DebugArtifact>,
    val timings: Map<String, Long>
)
