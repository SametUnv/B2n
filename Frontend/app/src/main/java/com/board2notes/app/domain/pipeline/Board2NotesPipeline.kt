package com.board2notes.app.domain.pipeline

import android.graphics.Bitmap
import com.board2notes.app.domain.engine.BoardEnhancementEngine
import com.board2notes.app.domain.engine.BoardSegmentationEngine
import com.board2notes.app.domain.engine.OcrEngine
import com.board2notes.app.domain.model.DebugArtifact
import com.board2notes.app.domain.model.EnhancementMode
import com.board2notes.app.domain.notes.NoteFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Board2NotesPipeline(
    private val segmentationEngine: BoardSegmentationEngine,
    private val enhancementEngine: BoardEnhancementEngine,
    private val ocrEngine: OcrEngine,
    private val noteFormatter: NoteFormatter
) {
    private val _state = MutableStateFlow<PipelineState>(PipelineState.Idle)
    val state: StateFlow<PipelineState> = _state.asStateFlow()

    suspend fun run(
        image: Bitmap,
        threshold: Float,
        enhancementMode: EnhancementMode
    ): PipelineResult {
        _state.value = PipelineState.ImageSelected(image)

        _state.value = PipelineState.DetectingBoard
        val board = segmentationEngine.detectBoard(image, threshold)
        _state.value = PipelineState.BoardDetected(board)

        _state.value = PipelineState.EnhancingImage
        val enhancement = enhancementEngine.enhance(board.cropBitmap, enhancementMode)
        _state.value = PipelineState.Enhanced(enhancement)

        _state.value = PipelineState.RunningOcr
        val ocr = ocrEngine.recognize(enhancement.ocrCandidateBitmap)
        _state.value = PipelineState.OcrCompleted(ocr)

        _state.value = PipelineState.FormattingNote
        val note = noteFormatter.format(ocr)

        val artifacts = mutableListOf<DebugArtifact>()
        artifacts += board.debugArtifacts
        artifacts += enhancement.debugArtifacts
        artifacts += DebugArtifact("ocr_raw_text", text = ocr.rawText)
        artifacts += DebugArtifact("formatted_note", text = "${note.title}\n\n${note.body}")

        val result = PipelineResult(
            originalImage = image,
            boardDetection = board,
            enhancement = enhancement,
            ocr = ocr,
            note = note,
            debugArtifacts = artifacts,
            timings = mapOf(
                "board_detection_ms" to board.elapsedMs,
                "enhancement_ms" to enhancement.elapsedMs,
                "ocr_ms" to ocr.elapsedMs
            )
        )
        _state.value = PipelineState.Completed(result)
        return result
    }

    fun reset() {
        _state.value = PipelineState.Idle
    }
}
