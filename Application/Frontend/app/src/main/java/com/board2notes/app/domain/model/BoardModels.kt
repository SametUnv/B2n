package com.board2notes.app.domain.model

/** Output mode the user picks after enhancement. */
enum class BoardMode {
    OCR_NOTE,
    VISUAL_NOTE
}

/** Result of the (mock) image enhancement step. */
data class EnhancementResult(
    val originalImageUri: String,
    val enhancedImageUri: String
)

/** Editable, OCR-style note produced from the board image. */
data class OcrNote(
    val text: String,
    val createdAt: String
)

/** Clean "notebook page" representation of the board content. */
data class VisualNote(
    val title: String,
    val lines: List<String>,
    val createdAt: String
)

/** Metadata for a generated export (mock PDF). */
data class ExportResult(
    val fileName: String,
    val createdAt: String,
    val mode: BoardMode
)
