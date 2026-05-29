package com.board2notes.app.data.settings

import com.board2notes.app.domain.model.EnhancementMode

enum class OcrEngineChoice {
    MlKitLatin,
    Mock,
    Trocr,
    Paddle
}

data class AppSettings(
    val threshold: Float = 0.5f,
    val debugMode: Boolean = false,
    val enhancementMode: EnhancementMode = EnhancementMode.Ocr,
    val ocrEngineChoice: OcrEngineChoice = OcrEngineChoice.MlKitLatin,
    val groqApiKey: String = "",
    val llmEnabled: Boolean = false
)
