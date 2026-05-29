package com.board2notes.app.domain.model

import android.graphics.Bitmap

enum class EnhancementMode {
    Ocr,
    VisualNote
}

data class DebugArtifact(
    val name: String,
    val bitmap: Bitmap? = null,
    val text: String? = null
)

data class BoardDetectionResult(
    val originalImage: Bitmap,
    val mask: BooleanMask,
    val maskBitmap: Bitmap,
    val overlayBitmap: Bitmap,
    val cropBitmap: Bitmap,
    val boundingBox: RectBox,
    val quad: Quad,
    val threshold: Float,
    val elapsedMs: Long,
    val warnings: List<String> = emptyList(),
    val debugArtifacts: List<DebugArtifact> = emptyList()
)

data class EnhancementResult(
    val inputBitmap: Bitmap,
    val enhancedBitmap: Bitmap,
    val ocrCandidateBitmap: Bitmap,
    val textLayerBitmap: Bitmap?,
    val mode: EnhancementMode,
    val elapsedMs: Long,
    val warnings: List<String> = emptyList(),
    val debugArtifacts: List<DebugArtifact> = emptyList()
)

data class OcrWord(
    val text: String,
    val boundingBox: RectBox?,
    val confidence: Float?
)

data class OcrLine(
    val text: String,
    val boundingBox: RectBox?,
    val confidence: Float?,
    val words: List<OcrWord>
)

data class OcrResult(
    val rawText: String,
    val lines: List<OcrLine>,
    val words: List<OcrWord>,
    val confidence: Float?,
    val elapsedMs: Long,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)

data class FormattedNote(
    val title: String,
    val body: String,
    val createdAtEpochMs: Long,
    val courseName: String = ""
)
