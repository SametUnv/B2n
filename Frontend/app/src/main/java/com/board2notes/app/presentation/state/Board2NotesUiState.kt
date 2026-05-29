package com.board2notes.app.presentation.state

import android.graphics.Bitmap
import com.board2notes.app.data.settings.AppSettings
import com.board2notes.app.domain.model.BoardDetectionResult
import com.board2notes.app.domain.model.EnhancementResult
import com.board2notes.app.domain.model.FormattedNote
import com.board2notes.app.domain.model.OcrResult
import com.board2notes.app.domain.model.Quad
import com.board2notes.app.domain.model.SavedNote
import com.board2notes.app.domain.pipeline.PipelineState
import java.io.File

enum class AppScreen {
    Home,
    Capture,
    Notes,
    NoteDetail,
    ImageReview,
    BoardDetection,
    Enhancement,
    Ocr,
    Note,
    Settings,
    About,
    DebugArtifacts
}

data class Board2NotesUiState(
    val screen: AppScreen = AppScreen.Home,
    val selectedImage: Bitmap? = null,
    val boardDetection: BoardDetectionResult? = null,
    val manualQuad: Quad? = null,
    val enhancement: EnhancementResult? = null,
    val ocrResult: OcrResult? = null,
    val note: FormattedNote? = null,
    val activeSavedNoteId: String? = null,
    val savedNotes: List<SavedNote> = emptyList(),
    val selectedSavedNote: SavedNote? = null,
    val settings: AppSettings = AppSettings(),
    val pipelineState: PipelineState = PipelineState.Idle,
    val isBusy: Boolean = false,
    val userMessage: String? = null,
    val debugFiles: List<File> = emptyList()
)
