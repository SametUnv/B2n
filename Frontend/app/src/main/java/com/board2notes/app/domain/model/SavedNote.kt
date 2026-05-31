package com.board2notes.app.domain.model

enum class NoteType {
    Text,
    Canvas
}

data class SavedNote(
    val id: String,
    val title: String,
    val body: String,
    val courseName: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val ocrText: String,
    val confidence: Float?,
    val cropImagePath: String?,
    val ocrImagePath: String?,
    val whitePageImagePath: String?,
    val noteType: NoteType = NoteType.Text,
    val canvasImagePath: String? = null,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false
) {
    val preview: String

        get() {

            val cleanText = body.substringBefore("[[B2N_CANVAS_V2]]")

                .substringBefore("[CanvasPages:")

                .substringBefore("[DrawingData:")

                .trim()

            if (cleanText.isNotBlank()) return cleanText.take(140)

            if (ocrText.isNotBlank()) return ocrText.trim().take(140)

            return if (noteType == NoteType.Canvas) "Canvas Çizim Notu" else ""

        }
}
