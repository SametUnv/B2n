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
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            threshold = prefs[Keys.threshold] ?: 0.5f,
            debugMode = prefs[Keys.debugMode] ?: false,
            enhancementMode = prefs[Keys.enhancementMode]?.let { runCatching { EnhancementMode.valueOf(it) }.getOrNull() }
                ?: EnhancementMode.Ocr,
            ocrEngineChoice = prefs[Keys.ocrEngine]?.let { runCatching { OcrEngineChoice.valueOf(it) }.getOrNull() }
                ?: OcrEngineChoice.MlKitLatin,
            groqApiKey = prefs[Keys.groqApiKey] ?: "",
            llmEnabled = prefs[Keys.llmEnabled] ?: false
        )
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
}
