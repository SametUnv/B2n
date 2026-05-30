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
    val isArchived: Boolean = false
) {
    val preview: String
        get() = body.substringBefore("[DrawingData:")
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.take(140)
            ?: if (noteType == NoteType.Canvas) "Canvas notu" else ""
}
