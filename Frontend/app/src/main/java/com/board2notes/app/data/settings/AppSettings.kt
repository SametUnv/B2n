package com.board2notes.app.data.settings

import com.board2notes.app.domain.model.EnhancementMode

enum class OcrEngineChoice {
    MlKitLatin,
    Mock,
    Trocr,
    Paddle
}

const val LAN_BACKEND_BASE_URL = "http://192.168.1.9:8000"
const val ADB_REVERSE_BACKEND_BASE_URL = "http://127.0.0.1:8000"
const val DEFAULT_BACKEND_BASE_URL = ADB_REVERSE_BACKEND_BASE_URL
const val EMULATOR_BACKEND_BASE_URL = "http://10.0.2.2:8000"

data class AppSettings(
    val threshold: Float = 0.5f,
    val debugMode: Boolean = false,
    val enhancementMode: EnhancementMode = EnhancementMode.Ocr,
    val ocrEngineChoice: OcrEngineChoice = OcrEngineChoice.MlKitLatin,
    val groqApiKey: String = "",
    val llmEnabled: Boolean = false,
    val backendBaseUrl: String = DEFAULT_BACKEND_BASE_URL,
    val useBackendPipeline: Boolean = true,
    val showBottomNavigation: Boolean = false,
    val allowFingerDrawing: Boolean = true,
    val useStylusPressure: Boolean = true,
    val palmRejection: Boolean = false,
    val defaultPenWidth: Float = 5f,
    val defaultEraserSize: Float = 32f,
    val canvasMinZoom: Float = 0.5f,
    val canvasMaxZoom: Float = 5f
)
