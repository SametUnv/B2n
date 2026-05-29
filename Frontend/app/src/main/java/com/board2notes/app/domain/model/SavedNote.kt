package com.board2notes.app.domain.model

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
    val whitePageImagePath: String?
) {
    val preview: String
        get() = body.lineSequence().firstOrNull { it.isNotBlank() }?.take(140).orEmpty()
}
