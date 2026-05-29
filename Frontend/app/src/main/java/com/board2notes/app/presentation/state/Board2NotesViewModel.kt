package com.board2notes.app.presentation.state

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.board2notes.app.AppContainer
import com.board2notes.app.Board2NotesApplication
import com.board2notes.app.data.image.BitmapLoader
import com.board2notes.app.data.image.BitmapPerspectiveCorrector
import com.board2notes.app.data.settings.OcrEngineChoice
import com.board2notes.app.domain.model.EnhancementMode
import com.board2notes.app.domain.model.FormattedNote
import com.board2notes.app.domain.model.PointF2
import com.board2notes.app.domain.model.Quad
import com.board2notes.app.domain.model.SavedNote
import com.board2notes.app.domain.pipeline.PipelineState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Board2NotesViewModel(
    application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(Board2NotesUiState())
    val uiState: StateFlow<Board2NotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
        refreshNotes()
    }

    fun openSettings() = setScreen(AppScreen.Settings)
    fun openNotes() = setScreen(AppScreen.Notes)
    fun openCapture() = setScreen(AppScreen.Capture)
    fun openAbout() = setScreen(AppScreen.About)
    fun openDebugArtifacts() = setScreen(AppScreen.DebugArtifacts)

    fun goHome() {
        _uiState.value = Board2NotesUiState(
            settings = _uiState.value.settings,
            savedNotes = _uiState.value.savedNotes
        )
    }

    fun goBack() {
        val target = when (_uiState.value.screen) {
            AppScreen.Home -> AppScreen.Home
            AppScreen.Capture -> AppScreen.Home
            AppScreen.Notes -> AppScreen.Home
            AppScreen.NoteDetail -> AppScreen.Notes
            AppScreen.ImageReview -> AppScreen.Home
            AppScreen.BoardDetection -> AppScreen.ImageReview
            AppScreen.Enhancement -> AppScreen.BoardDetection
            AppScreen.Ocr -> AppScreen.Enhancement
            AppScreen.Note -> AppScreen.Enhancement
            AppScreen.Settings -> AppScreen.Home
            AppScreen.About -> AppScreen.Settings
            AppScreen.DebugArtifacts -> AppScreen.Note
        }
        setScreen(target)
    }

    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            busy(PipelineState.Idle)
            runCatching {
                withContext(Dispatchers.IO) { BitmapLoader.decodeFromUri(getApplication(), uri) }
            }.onSuccess { bitmap ->
                _uiState.value = _uiState.value.copy(
                    selectedImage = bitmap,
                    boardDetection = null,
                    enhancement = null,
                    ocrResult = null,
                    note = null,
                    activeSavedNoteId = null,
                    selectedSavedNote = null,
                    manualQuad = null,
                    screen = AppScreen.ImageReview,
                    pipelineState = PipelineState.ImageSelected(bitmap),
                    isBusy = false,
                    userMessage = null
                )
            }.onFailure { error ->
                fail("Goruntu okunamadi: ${error.message}", error)
            }
        }
    }

    fun rotateSelected(degrees: Float) {
        val bitmap = _uiState.value.selectedImage ?: return
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        _uiState.value = _uiState.value.copy(
            selectedImage = rotated,
            boardDetection = null,
            enhancement = null,
            ocrResult = null,
            note = null,
            activeSavedNoteId = null,
            selectedSavedNote = null,
            pipelineState = PipelineState.ImageSelected(rotated)
        )
    }

    fun detectBoard() {
        val image = _uiState.value.selectedImage ?: return
        viewModelScope.launch {
            busy(PipelineState.DetectingBoard, AppScreen.BoardDetection)
            runCatching {
                container.segmentationEngine.detectBoard(image, _uiState.value.settings.threshold)
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    boardDetection = result,
                    manualQuad = result.quad,
                    screen = AppScreen.BoardDetection,
                    pipelineState = PipelineState.BoardDetected(result),
                    isBusy = false,
                    userMessage = result.warnings.firstOrNull()
                )
            }.onFailure { error ->
                fail("Tahta tespit edilemedi: ${error.message}", error)
            }
        }
    }

    fun setThreshold(value: Float) {
        viewModelScope.launch {
            container.settingsRepository.setThreshold(value)
        }
    }

    fun updateManualCorner(index: Int, x: Float, y: Float) {
        val current = _uiState.value.manualQuad ?: _uiState.value.boardDetection?.quad ?: return
        val points = listOf(current.topLeft, current.topRight, current.bottomRight, current.bottomLeft).toMutableList()
        if (index !in points.indices) return
        points[index] = PointF2(x, y)
        _uiState.value = _uiState.value.copy(
            manualQuad = Quad(points[0], points[1], points[2], points[3])
        )
    }

    fun applyManualPerspective() {
        val detection = _uiState.value.boardDetection ?: return
        val quad = _uiState.value.manualQuad ?: detection.quad
        viewModelScope.launch {
            busy(PipelineState.BoardDetected(detection), AppScreen.BoardDetection)
            runCatching {
                withContext(Dispatchers.Default) {
                    BitmapPerspectiveCorrector.correct(detection.originalImage, quad)
                }
            }.onSuccess { corrected ->
                val updated = detection.copy(
                    cropBitmap = corrected,
                    quad = quad,
                    warnings = detection.warnings + "Elle duzeltilmis kirpma uygulandi."
                )
                _uiState.value = _uiState.value.copy(
                    boardDetection = updated,
                    manualQuad = quad,
                    pipelineState = PipelineState.BoardDetected(updated),
                    isBusy = false,
                    userMessage = "Elle duzeltilmis kirpma hazir."
                )
            }.onFailure { error ->
                fail("Elle duzeltme uygulanamadi: ${error.message}", error)
            }
        }
    }

    fun enhanceBoard() {
        val crop = _uiState.value.boardDetection?.cropBitmap ?: return
        viewModelScope.launch {
            busy(PipelineState.EnhancingImage, AppScreen.Enhancement)
            runCatching {
                container.enhancementEngine.enhance(crop, _uiState.value.settings.enhancementMode)
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    enhancement = result,
                    screen = AppScreen.Enhancement,
                    pipelineState = PipelineState.Enhanced(result),
                    isBusy = false,
                    userMessage = result.warnings.firstOrNull()
                )
            }.onFailure { error ->
                fail("Goruntu iyilestirilemedi: ${error.message}", error)
            }
        }
    }

    fun runOcrAndFormat() {
        val image = _uiState.value.enhancement?.ocrCandidateBitmap ?: return
        viewModelScope.launch {
            busy(PipelineState.RunningOcr, AppScreen.Ocr)
            runCatching {
                val ocr = container.ocrEngine.recognize(image)
                val note = container.noteFormatter.format(ocr)
                val saved = container.noteRepository.createFromPipeline(
                    note = note,
                    ocrResult = ocr,
                    cropBitmap = _uiState.value.boardDetection?.cropBitmap,
                    ocrBitmap = _uiState.value.enhancement?.ocrCandidateBitmap,
                    whitePageBitmap = _uiState.value.enhancement?.textLayerBitmap
                )
                Triple(ocr, note, saved)
            }.onSuccess { (ocr, note, saved) ->
                _uiState.value = _uiState.value.copy(
                    ocrResult = ocr,
                    note = note,
                    activeSavedNoteId = saved.id,
                    selectedSavedNote = saved,
                    savedNotes = loadNotesSafely(),
                    screen = AppScreen.Note,
                    pipelineState = PipelineState.Completed(
                        com.board2notes.app.domain.pipeline.PipelineResult(
                            originalImage = _uiState.value.selectedImage,
                            boardDetection = _uiState.value.boardDetection,
                            enhancement = _uiState.value.enhancement,
                            ocr = ocr,
                            note = note,
                            debugArtifacts = buildDebugArtifacts(ocr.rawText, note),
                            timings = mapOf(
                                "board_detection_ms" to (_uiState.value.boardDetection?.elapsedMs ?: 0L),
                                "enhancement_ms" to (_uiState.value.enhancement?.elapsedMs ?: 0L),
                                "ocr_ms" to ocr.elapsedMs
                            )
                        )
                    ),
                    isBusy = false,
                    userMessage = ocr.warnings.firstOrNull() ?: "Not kaydedildi."
                )
            }.onFailure { error ->
                fail("OCR calistirilamadi: ${error.message}", error)
            }
        }
    }

    fun skipToNoteEditor() {
        val note = FormattedNote(
            title = "Yeni Not",
            body = "",
            courseName = "",
            createdAtEpochMs = System.currentTimeMillis()
        )
        viewModelScope.launch {
            runCatching {
                container.noteRepository.createFromPipeline(
                    note = note,
                    ocrResult = com.board2notes.app.domain.model.OcrResult(
                        rawText = "", lines = emptyList(), words = emptyList(),
                        confidence = null, elapsedMs = 0
                    ),
                    cropBitmap = _uiState.value.boardDetection?.cropBitmap,
                    ocrBitmap = null,
                    whitePageBitmap = null
                )
            }.onSuccess { saved ->
                _uiState.value = _uiState.value.copy(
                    note = note,
                    activeSavedNoteId = saved.id,
                    selectedSavedNote = saved,
                    savedNotes = loadNotesSafely(),
                    isBusy = false
                )
            }.onFailure { error ->
                fail("Not olusturulamadi: ${error.message}", error)
            }
        }
    }

    fun updateNote(title: String, body: String, courseName: String) {
        val current = _uiState.value.note ?: return
        _uiState.value = _uiState.value.copy(
            note = current.copy(title = title, body = body, courseName = courseName)
        )
    }

    fun saveCurrentNote(onSaved: (() -> Unit)? = null) {
        val note = _uiState.value.note ?: return
        val id = _uiState.value.activeSavedNoteId ?: return
        viewModelScope.launch {
            runCatching {
                container.noteRepository.updateContent(
                    id = id,
                    title = note.title,
                    body = note.body,
                    courseName = note.courseName
                )
            }.onSuccess { saved ->
                _uiState.value = _uiState.value.copy(
                    selectedSavedNote = saved,
                    savedNotes = loadNotesSafely(),
                    userMessage = "Not kaydedildi."
                )
                onSaved?.invoke()
            }.onFailure { error ->
                fail("Not kaydedilemedi: ${error.message}", error)
            }
        }
    }

    fun refreshNotes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(savedNotes = loadNotesSafely())
        }
    }

    fun openSavedNote(id: String) {
        viewModelScope.launch {
            val saved = container.noteRepository.getNote(id)
            if (saved == null) {
                _uiState.value = _uiState.value.copy(userMessage = "Not bulunamadi.")
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                selectedSavedNote = saved,
                activeSavedNoteId = saved.id,
                note = saved.toFormattedNote(),
                screen = AppScreen.NoteDetail
            )
        }
    }

    fun deleteSelectedNote() {
        val id = _uiState.value.selectedSavedNote?.id ?: return
        viewModelScope.launch {
            runCatching {
                container.noteRepository.deleteNote(id)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    selectedSavedNote = null,
                    activeSavedNoteId = null,
                    note = null,
                    savedNotes = loadNotesSafely(),
                    screen = AppScreen.Notes,
                    userMessage = "Not silindi."
                )
            }.onFailure { error ->
                fail("Not silinemedi: ${error.message}", error)
            }
        }
    }

    fun exportDebugArtifacts() {
        val note = _uiState.value.note
        val ocr = _uiState.value.ocrResult
        val artifacts = buildDebugArtifacts(ocr?.rawText.orEmpty(), note)
        viewModelScope.launch {
            runCatching {
                container.debugArtifactWriter.write(artifacts)
            }.onSuccess { files ->
                _uiState.value = _uiState.value.copy(
                    debugFiles = files,
                    userMessage = "${files.size} debug ciktisi kaydedildi."
                )
            }.onFailure { error ->
                fail("Debug ciktilari kaydedilemedi: ${error.message}", error)
            }
        }
    }

    fun setDebugMode(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setDebugMode(enabled) }
    }

    fun setEnhancementMode(mode: EnhancementMode) {
        viewModelScope.launch { container.settingsRepository.setEnhancementMode(mode) }
    }

    fun setOcrEngineChoice(choice: OcrEngineChoice) {
        viewModelScope.launch { container.settingsRepository.setOcrEngineChoice(choice) }
    }

    fun setLlmEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setLlmEnabled(enabled) }
    }

    fun setGroqApiKey(value: String) {
        viewModelScope.launch { container.settingsRepository.setGroqApiKey(value) }
    }

    fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(userMessage = message)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }

    private fun buildDebugArtifacts(rawText: String, note: FormattedNote?) =
        buildList {
            _uiState.value.boardDetection?.debugArtifacts?.let { addAll(it) }
            _uiState.value.enhancement?.debugArtifacts?.let { addAll(it) }
            add(com.board2notes.app.domain.model.DebugArtifact("ocr_raw_text", text = rawText))
            note?.let { add(com.board2notes.app.domain.model.DebugArtifact("formatted_note", text = "${it.title}\n\n${it.body}")) }
        }

    private suspend fun loadNotesSafely(): List<SavedNote> =
        runCatching { container.noteRepository.listNotes() }.getOrDefault(emptyList())

    private fun SavedNote.toFormattedNote(): FormattedNote = FormattedNote(
        title = title,
        body = body,
        courseName = courseName,
        createdAtEpochMs = createdAtEpochMs
    )

    private fun busy(state: PipelineState, screen: AppScreen? = null) {
        _uiState.value = _uiState.value.copy(
            pipelineState = state,
            screen = screen ?: _uiState.value.screen,
            isBusy = true,
            userMessage = null
        )
    }

    private fun fail(message: String, error: Throwable? = null) {
        _uiState.value = _uiState.value.copy(
            pipelineState = PipelineState.Error(message, error),
            isBusy = false,
            userMessage = message
        )
    }

    private fun setScreen(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(screen = screen)
    }

    class Factory(
        private val application: Board2NotesApplication
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return Board2NotesViewModel(application, application.container) as T
        }
    }
}
