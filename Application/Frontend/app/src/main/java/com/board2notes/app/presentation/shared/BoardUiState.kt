package com.board2notes.app.presentation.shared

import com.board2notes.app.domain.model.BoardMode

/**
 * Single source of truth for the whole capture → note flow.
 * Mirrors the state shape from the product spec, expanded with a few
 * fields needed to drive the processing and export screens.
 */
data class BoardUiState(
    val selectedImageUri: String? = null,
    val originalImageUrl: String? = null,
    val enhancedImageUrl: String? = null,
    val mockOcrText: String = "",
    val visualNoteTitle: String = "",
    val visualNoteLines: List<String> = emptyList(),
    val isProcessing: Boolean = false,
    val processingStepIndex: Int = 0,
    val selectedMode: BoardMode? = null,
    val exportFileName: String? = null,
    val exportDate: String? = null,
    val errorMessage: String? = null
) {
    val hasImage: Boolean get() = selectedImageUri != null
}

/** Ordered labels shown on the Processing screen. */
val ProcessingSteps: List<String> = listOf(
    "Görsel hazırlanıyor",
    "Tahta alanı analiz ediliyor",
    "Görüntü iyileştirme hazırlanıyor",
    "Not modu oluşturuluyor"
)
