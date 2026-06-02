package com.board2notes.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.board2notes.app.domain.model.EnhancementMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "board2notes_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val threshold = floatPreferencesKey("threshold")
        val debugMode = booleanPreferencesKey("debug_mode")
        val enhancementMode = stringPreferencesKey("enhancement_mode")
        val ocrEngine = stringPreferencesKey("ocr_engine")
        val groqApiKey = stringPreferencesKey("groq_api_key")
        val llmEnabled = booleanPreferencesKey("llm_enabled")
        val backendBaseUrl = stringPreferencesKey("backend_base_url")
        val backendDeviceMigration = booleanPreferencesKey("backend_device_migration_20260530")
        val backendAdbReverseMigration = booleanPreferencesKey("backend_adb_reverse_migration_20260530")
        val useBackendPipeline = booleanPreferencesKey("use_backend_pipeline")
        val showBottomNavigation = booleanPreferencesKey("show_bottom_navigation")
        val allowFingerDrawing = booleanPreferencesKey("allow_finger_drawing")
        val useStylusPressure = booleanPreferencesKey("use_stylus_pressure")
        val palmRejection = booleanPreferencesKey("palm_rejection")
        val defaultPenWidth = floatPreferencesKey("default_pen_width")
        val defaultEraserSize = floatPreferencesKey("default_eraser_size")
        val canvasMinZoom = floatPreferencesKey("canvas_min_zoom")
        val canvasMaxZoom = floatPreferencesKey("canvas_max_zoom")
        val inkDarkness = floatPreferencesKey("ink_darkness")
        val useDarkTheme = booleanPreferencesKey("use_dark_theme")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val storedBackendBaseUrl = prefs[Keys.backendBaseUrl]?.trim().orEmpty()
        AppSettings(
            threshold = prefs[Keys.threshold] ?: 0.5f,
            debugMode = prefs[Keys.debugMode] ?: false,
            enhancementMode = prefs[Keys.enhancementMode]?.let { runCatching { EnhancementMode.valueOf(it) }.getOrNull() }
                ?: EnhancementMode.Ocr,
            ocrEngineChoice = prefs[Keys.ocrEngine]?.let { runCatching { OcrEngineChoice.valueOf(it) }.getOrNull() }
                ?: OcrEngineChoice.MlKitLatin,
            groqApiKey = prefs[Keys.groqApiKey] ?: "",
            llmEnabled = prefs[Keys.llmEnabled] ?: false,
            backendBaseUrl = storedBackendBaseUrl.ifBlank { DEFAULT_BACKEND_BASE_URL },
            useBackendPipeline = prefs[Keys.useBackendPipeline] ?: true,
            showBottomNavigation = prefs[Keys.showBottomNavigation] ?: false,
            allowFingerDrawing = prefs[Keys.allowFingerDrawing] ?: true,
            useStylusPressure = prefs[Keys.useStylusPressure] ?: true,
            palmRejection = prefs[Keys.palmRejection] ?: false,
            defaultPenWidth = prefs[Keys.defaultPenWidth] ?: 30f,
            defaultEraserSize = prefs[Keys.defaultEraserSize] ?: 32f,
            canvasMinZoom = prefs[Keys.canvasMinZoom] ?: 0.5f,
            canvasMaxZoom = prefs[Keys.canvasMaxZoom] ?: 5f,
            inkDarkness = prefs[Keys.inkDarkness] ?: 0.6f,
            useDarkTheme = prefs[Keys.useDarkTheme] ?: false
        )
    }

    suspend fun migrateBackendBaseUrlForPhysicalDevice() {
        context.settingsDataStore.edit { prefs ->
            if (prefs[Keys.backendDeviceMigration] == true) return@edit
            val current = prefs[Keys.backendBaseUrl]?.trim()
            if (current.isNullOrBlank()) {
                prefs[Keys.backendBaseUrl] = DEFAULT_BACKEND_BASE_URL
            }
            prefs[Keys.backendDeviceMigration] = true
        }
        context.settingsDataStore.edit { prefs ->
            if (prefs[Keys.backendAdbReverseMigration] == true) return@edit
            val current = prefs[Keys.backendBaseUrl]?.trim()
            if (current.isNullOrBlank()) {
                prefs[Keys.backendBaseUrl] = ADB_REVERSE_BACKEND_BASE_URL
            }
            prefs[Keys.backendAdbReverseMigration] = true
        }
    }

    suspend fun setThreshold(value: Float) {
        context.settingsDataStore.edit { it[Keys.threshold] = value.coerceIn(0.45f, 0.60f) }
    }

    suspend fun setDebugMode(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.debugMode] = value }
    }

    suspend fun setEnhancementMode(value: EnhancementMode) {
        context.settingsDataStore.edit { it[Keys.enhancementMode] = value.name }
    }

    suspend fun setOcrEngineChoice(value: OcrEngineChoice) {
        context.settingsDataStore.edit { it[Keys.ocrEngine] = value.name }
    }

    suspend fun setGroqApiKey(value: String) {
        context.settingsDataStore.edit { it[Keys.groqApiKey] = value }
    }

    suspend fun setLlmEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.llmEnabled] = value }
    }

    suspend fun setBackendBaseUrl(value: String) {
        context.settingsDataStore.edit { it[Keys.backendBaseUrl] = value.trim() }
    }

    suspend fun setUseBackendPipeline(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.useBackendPipeline] = value }
    }

    suspend fun setShowBottomNavigation(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.showBottomNavigation] = value }
    }

    suspend fun setAllowFingerDrawing(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.allowFingerDrawing] = value }
    }

    suspend fun setUseStylusPressure(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.useStylusPressure] = value }
    }

    suspend fun setPalmRejection(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.palmRejection] = value }
    }

    suspend fun setDefaultPenWidth(value: Float) {
        context.settingsDataStore.edit { it[Keys.defaultPenWidth] = value.coerceIn(1f, 100f) }
    }

    suspend fun setDefaultEraserSize(value: Float) {
        context.settingsDataStore.edit { it[Keys.defaultEraserSize] = value.coerceIn(12f, 70f) }
    }

    suspend fun setInkDarkness(value: Float) {
        context.settingsDataStore.edit { it[Keys.inkDarkness] = value.coerceIn(0f, 1f) }
    }

    suspend fun setCanvasZoomRange(minZoom: Float, maxZoom: Float) {
        val safeMin = minZoom.coerceIn(0.35f, 1f)
        val safeMax = maxZoom.coerceIn(2f, 8f).coerceAtLeast(safeMin + 1f)
        context.settingsDataStore.edit {
            it[Keys.canvasMinZoom] = safeMin
            it[Keys.canvasMaxZoom] = safeMax
        }
    }

    suspend fun setUseDarkTheme(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.useDarkTheme] = value }
    }
}
