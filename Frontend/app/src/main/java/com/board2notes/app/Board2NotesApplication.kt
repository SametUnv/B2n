package com.board2notes.app

import android.app.Application
import com.board2notes.app.data.backend.BackendBoard2NotesClient
import com.board2notes.app.data.image.DebugArtifactWriter
import com.board2notes.app.data.ml.FallbackBoardSegmentationEngine
import com.board2notes.app.data.ml.HeuristicEnhancementEngine
import com.board2notes.app.data.models.ModelAssetManager
import com.board2notes.app.data.models.ModelManifestReader
import com.board2notes.app.data.notes.FileNoteRepository
import com.board2notes.app.data.notes.RuleBasedNoteFormatter
import com.board2notes.app.data.ocr.MlKitLatinOcrEngine
import com.board2notes.app.data.ocr.SafeOcrEngine
import com.board2notes.app.data.settings.SettingsRepository
import com.board2notes.app.domain.engine.BoardEnhancementEngine
import com.board2notes.app.domain.engine.BoardSegmentationEngine
import com.board2notes.app.domain.engine.OcrEngine
import com.board2notes.app.domain.notes.NoteFormatter
import com.board2notes.app.domain.notes.NoteRepository

class Board2NotesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val modelAssetManager: ModelAssetManager = ModelAssetManager(application)
    val modelManifestReader: ModelManifestReader = ModelManifestReader(application)
    val settingsRepository: SettingsRepository = SettingsRepository(application)
    val debugArtifactWriter: DebugArtifactWriter = DebugArtifactWriter(application)
    val backendClient: BackendBoard2NotesClient = BackendBoard2NotesClient()
    val noteRepository: NoteRepository = FileNoteRepository(application.filesDir)
    val segmentationEngine: BoardSegmentationEngine = FallbackBoardSegmentationEngine()
    val enhancementEngine: BoardEnhancementEngine = HeuristicEnhancementEngine()
    val ocrEngine: OcrEngine = SafeOcrEngine(MlKitLatinOcrEngine())
    val noteFormatter: NoteFormatter = RuleBasedNoteFormatter()
}
