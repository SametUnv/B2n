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
import com.board2notes.app.data.backend.BackendPipelineResult
import com.board2notes.app.data.image.BitmapLoader
import com.board2notes.app.data.image.BitmapPerspectiveCorrector
import com.board2notes.app.data.image.InkDarkener
import com.board2notes.app.data.settings.OcrEngineChoice
import com.board2notes.app.domain.model.EnhancementMode
import com.board2notes.app.domain.model.FormattedNote
import com.board2notes.app.domain.model.NoteType
import com.board2notes.app.domain.model.OcrResult
import com.board2notes.app.domain.model.PointF2
import com.board2notes.app.domain.model.Quad
import com.board2notes.app.domain.model.SavedNote
import com.board2notes.app.domain.pipeline.PipelineState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class AppliedResult(
    val note: SavedNote,
    val canvasInsertAsset: String?,
    val canvasInsertJobId: String? = null
)

class Board2NotesViewModel(
    application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(Board2NotesUiState())
    val uiState: StateFlow<Board2NotesUiState> = _uiState.asStateFlow()
    private var pendingScanJob: Job? = null

    init {
        viewModelScope.launch {
            container.settingsRepository.migrateBackendBaseUrlForPhysicalDevice()
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
            savedNotes = _uiState.value.savedNotes,
            archivedNotes = _uiState.value.archivedNotes,
            deletedNotes = _uiState.value.deletedNotes,
            favoriteNotes = _uiState.value.favoriteNotes
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
            val previous = _uiState.value
            val keepActiveNote = previous.pendingScanNoteId != null
            runCatching {
                withContext(Dispatchers.IO) { BitmapLoader.decodeFromUri(getApplication(), uri) }
            }.onSuccess { bitmap ->
                _uiState.value = _uiState.value.copy(
                    selectedImage = bitmap,
                    boardDetection = null,
                    enhancement = null,
                    ocrResult = null,
                    note = if (keepActiveNote) previous.note else null,
                    activeSavedNoteId = if (keepActiveNote) previous.activeSavedNoteId else null,
                    selectedSavedNote = if (keepActiveNote) previous.selectedSavedNote else null,
                    manualQuad = null,
                    screen = AppScreen.ImageReview,
                    pipelineState = PipelineState.ImageSelected(bitmap),
                    isBusy = false,
                    userMessage = null
                )
            }.onFailure { error ->
                fail("Görüntü okunamadı: ${error.message}", error)
            }
        }
    }

    fun rotateSelected(degrees: Float) {
        val bitmap = _uiState.value.selectedImage ?: return
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val keepActiveNote = _uiState.value.pendingScanNoteId != null
        _uiState.value = _uiState.value.copy(
            selectedImage = rotated,
            boardDetection = null,
            enhancement = null,
            ocrResult = null,
            note = if (keepActiveNote) _uiState.value.note else null,
            activeSavedNoteId = if (keepActiveNote) _uiState.value.activeSavedNoteId else null,
            selectedSavedNote = if (keepActiveNote) _uiState.value.selectedSavedNote else null,
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
                    warnings = detection.warnings + "Elle düzeltilmiş kırpma uygulandı."
                )
                _uiState.value = _uiState.value.copy(
                    boardDetection = updated,
                    manualQuad = quad,
                    pipelineState = PipelineState.BoardDetected(updated),
                    isBusy = false,
                    userMessage = "Elle düzeltilmiş kırpma hazır"
                )
            }.onFailure { error ->
                fail("Elle düzeltme uygulanamadı: ${error.message}", error)
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
                fail("Görüntü iyileştirilemedi: ${error.message}", error)
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
                    archivedNotes = loadArchivedNotesSafely(),
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
                fail("OCR çalıştırılamadı: ${error.message}", error)
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
                    archivedNotes = loadArchivedNotesSafely(),
                    isBusy = false
                )
            }.onFailure { error ->
                fail("Not oluşturulamadı: ${error.message}", error)
            }
        }
    }

    fun createBlankNote(noteType: NoteType, courseName: String = "", onCreated: (() -> Unit)? = null) {
        viewModelScope.launch {
            busy(PipelineState.Idle, AppScreen.Note)
            runCatching {
                val saved = container.noteRepository.createBlank(noteType)
                if (courseName.isNotBlank()) {
                    container.noteRepository.updateContent(
                        id = saved.id,
                        title = saved.title,
                        body = saved.body,
                        courseName = courseName
                    ) ?: saved
                } else {
                    saved
                }
            }.onSuccess { saved ->
                _uiState.value = _uiState.value.copy(
                    note = saved.toFormattedNote(),
                    activeSavedNoteId = saved.id,
                    selectedSavedNote = saved,
                    savedNotes = loadNotesSafely(),
                    archivedNotes = loadArchivedNotesSafely(),
                    pendingScanNoteId = null,
                    pendingScanNoteType = null,
                    pendingScanPageIndex = null,
                    screen = AppScreen.Note,
                    isBusy = false,
                    userMessage = if (noteType == NoteType.Canvas) "Canvas notu oluşturuldu." else "Yazı notu oluşturuldu."
                )
                onCreated?.invoke()
            }.onFailure { error ->
                fail("Not oluşturulamadı: ${error.message}", error)
            }
        }
    }

    fun startScanForCurrentNote(pageIndex: Int = 0) {
        pendingScanJob?.cancel()
        pendingScanJob = null
        val selected = _uiState.value.selectedSavedNote
        val id = selected?.id ?: _uiState.value.activeSavedNoteId
        if (id == null) {
            _uiState.value = _uiState.value.copy(userMessage = "Tarama eklemek için önce bir not açın.")
            return
        }
        val noteType = selected?.noteType ?: NoteType.Text
        _uiState.value = _uiState.value.copy(
            pendingScanNoteId = id,
            pendingScanNoteType = noteType,
            pendingScanPageIndex = pageIndex,
            screen = AppScreen.Capture,
            userMessage = "Tahtayı seçin; çıktı bu nota eklenecek."
        )
    }

    fun runBackendPipelineForPendingNote(onCompleted: (() -> Unit)? = null) {
        if (pendingScanJob?.isActive == true) return
        val image = _uiState.value.selectedImage
        val noteId = _uiState.value.pendingScanNoteId
        val noteType = _uiState.value.pendingScanNoteType ?: _uiState.value.selectedSavedNote?.noteType ?: NoteType.Text
        val pageIndex = _uiState.value.pendingScanPageIndex ?: 0
        if (image == null || noteId == null) {
            _uiState.value = _uiState.value.copy(userMessage = "İşlenecek görsel veya hedef not bulunamadı.")
            return
        }
        if (!_uiState.value.settings.useBackendPipeline) {
            _uiState.value = _uiState.value.copy(userMessage = "Not içine tarama için Backend API ayarını açın.")
            return
        }
        val job = viewModelScope.launch {
            busy(PipelineState.RunningOcr, AppScreen.Enhancement)
            val progressStartedAt = System.currentTimeMillis()
            val progressJob = launch {
                val ocrStep = if (noteType == NoteType.Text) "OCR metni çıkarılıyor." else "Beyaz sayfa çıktısı hazırlanıyor."
                val steps = listOf(
                    "Onaylanan tahta alanı hazırlanıyor.",
                    "Model 2 görüntüyü iyileştiriyor.",
                    ocrStep
                )
                steps.forEachIndexed { index, message ->
                    _uiState.value = _uiState.value.copy(
                        loadingMessage = message,
                        screen = when (index) {
                            0 -> AppScreen.BoardDetection
                            1 -> AppScreen.Enhancement
                            else -> AppScreen.Ocr
                        }
                    )
                    delay(850)
                }
            }
            runCatching {
                val settings = _uiState.value.settings
                val approvedQuad = _uiState.value.manualQuad ?: _uiState.value.boardDetection?.quad
                val result = if (approvedQuad != null) {
                    container.backendClient.runPipelineFromQuad(
                        bitmap = image,
                        quad = approvedQuad,
                        baseUrl = settings.backendBaseUrl,
                        runOcr = noteType == NoteType.Text
                    )
                } else {
                    container.backendClient.runPipeline(
                        bitmap = image,
                        baseUrl = settings.backendBaseUrl,
                        threshold = settings.threshold,
                        runOcr = noteType == NoteType.Text
                    )
                }
                val minimumProgressMs = if (noteType == NoteType.Text) 3000L else 2400L
                val remainingProgressMs = minimumProgressMs - (System.currentTimeMillis() - progressStartedAt)
                if (remainingProgressMs > 0) delay(remainingProgressMs)
                applyBackendResultToNote(noteId, noteType, result)
            }.onSuccess { applied ->
                progressJob.cancel()
                val saved = applied.note
                _uiState.value = _uiState.value.copy(
                    selectedSavedNote = saved,
                    activeSavedNoteId = saved.id,
                    note = saved.toFormattedNote(),
                    savedNotes = loadNotesSafely(),
                    archivedNotes = loadArchivedNotesSafely(),
                    ocrResult = OcrResult(
                        rawText = saved.ocrText,
                        lines = emptyList(),
                        words = emptyList(),
                        confidence = saved.confidence,
                        elapsedMs = 0L
                    ),
                    pendingScanNoteId = null,
                    pendingScanNoteType = null,
                    pendingScanPageIndex = null,
                    boardDetection = null,
                    manualQuad = null,
                    pendingCanvasImageAsset = applied.canvasInsertAsset,
                    pendingCanvasImagePageIndex = applied.canvasInsertAsset?.let { pageIndex },
                    pendingCanvasImageJobId = applied.canvasInsertJobId,
                    screen = AppScreen.Note,
                    isBusy = false,
                    loadingMessage = null,
                    userMessage = if (saved.noteType == NoteType.Canvas) {
                        "Tahta çıktısı not içine eklendi; konumlandırıp boyutlandırabilirsin."
                    } else {
                        "OCR metni yazı notuna eklendi."
                    }
                )
                onCompleted?.invoke()
            }.onFailure { error ->
                progressJob.cancel()
                if (error is CancellationException) return@onFailure
                _uiState.value = _uiState.value.copy(
                    pendingScanNoteId = null,
                    pendingScanNoteType = null,
                    pendingScanPageIndex = null,
                    loadingMessage = null
                )
                fail("Backend pipeline çalıştırılamadı: ${error.message}", error)
            }
        }
        pendingScanJob = job
        job.invokeOnCompletion {
            if (pendingScanJob === job) pendingScanJob = null
        }
    }

    fun detectBackendBoardForPendingNote(onCompleted: (() -> Unit)? = null) {
        if (pendingScanJob?.isActive == true) return
        val image = _uiState.value.selectedImage
        val noteId = _uiState.value.pendingScanNoteId
        if (image == null || noteId == null) {
            _uiState.value = _uiState.value.copy(userMessage = "Köşe tespiti için görsel veya hedef not bulunamadı.")
            return
        }
        if (!_uiState.value.settings.useBackendPipeline) {
            _uiState.value = _uiState.value.copy(userMessage = "Köşe tespiti için Backend API ayarını açın.")
            return
        }
        val job = viewModelScope.launch {
            busy(PipelineState.DetectingBoard, AppScreen.BoardDetection)
            _uiState.value = _uiState.value.copy(loadingMessage = "Model 1 tahta köşelerini tespit ediyor.")
            runCatching {
                val settings = _uiState.value.settings
                container.backendClient.detectBoard(
                    bitmap = image,
                    baseUrl = settings.backendBaseUrl,
                    threshold = settings.threshold
                )
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    boardDetection = result,
                    manualQuad = result.quad,
                    screen = AppScreen.BoardDetection,
                    pipelineState = PipelineState.BoardDetected(result),
                    isBusy = false,
                    loadingMessage = null,
                    userMessage = "Köşeleri kontrol edip gerekirse düzenleyin."
                )
                onCompleted?.invoke()
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                _uiState.value = _uiState.value.copy(loadingMessage = null)
                fail("Backend Model 1 tespiti çalıştırılamadı: ${error.message}", error)
            }
        }
        pendingScanJob = job
        job.invokeOnCompletion {
            if (pendingScanJob === job) pendingScanJob = null
        }
    }

    fun updateNote(title: String, body: String, courseName: String) {
        val current = _uiState.value.note ?: return
        _uiState.value = _uiState.value.copy(
            note = current.copy(title = title, body = body, courseName = courseName)
        )
    }

    fun saveCurrentNote(
        onSaved: (() -> Unit)? = null,
        showMessage: Boolean = true,
        onFailed: (() -> Unit)? = null
    ) {
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
                _uiState.value = withRefreshedNotes(_uiState.value.copy(
                    selectedSavedNote = saved,
                    userMessage = if (showMessage) "Not kaydedildi." else _uiState.value.userMessage
                ))
                onSaved?.invoke()
            }.onFailure { error ->
                onFailed?.invoke()
                fail("Not kaydedilemedi: ${error.message}", error)
            }
        }
    }

    fun autoSaveCurrentNote() {
        saveCurrentNote(showMessage = false)
    }

    fun refreshNotes() {
        viewModelScope.launch {
            _uiState.value = withRefreshedNotes()
        }
    }

    fun openSavedNote(id: String) {
        viewModelScope.launch {
            val saved = container.noteRepository.getNote(id)
            if (saved == null) {
                _uiState.value = _uiState.value.copy(userMessage = "Not bulunamadı.")
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
                _uiState.value = withRefreshedNotes(_uiState.value.copy(
                    selectedSavedNote = null,
                    activeSavedNoteId = null,
                    note = null,
                    screen = AppScreen.Notes,
                    userMessage = "Not silindi."
                ))
            }.onFailure { error ->
                fail("Not silinemedi: ${error.message}", error)
            }
        }
    }

    fun deleteNotes(ids: Set<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                ids.forEach { id -> container.noteRepository.deleteNote(id) }
            }.onSuccess {
                _uiState.value = withRefreshedNotes(_uiState.value.copy(
                    userMessage = "${ids.size} not silindi."
                ))
            }.onFailure { error ->
                fail("Notlar silinemedi: ${error.message}", error)
            }
        }
    }

    fun deleteNoteById(id: String) {
        viewModelScope.launch {
            runCatching {
                container.noteRepository.deleteNote(id)
            }.onSuccess {
                _uiState.value = withRefreshedNotes(_uiState.value.copy(
                    userMessage = "Not silindi."
                ))
            }.onFailure { error ->
                fail("Not silinemedi: ${error.message}", error)
            }
        }
    }

    fun updateNoteCourse(id: String, courseName: String) {
        viewModelScope.launch {
            val current = container.noteRepository.getNote(id) ?: return@launch
            runCatching {
                container.noteRepository.updateContent(
                    id = id,
                    title = current.title,
                    body = current.body,
                    courseName = courseName
                )
            }.onSuccess { saved ->
                _uiState.value = withRefreshedNotes(_uiState.value.copy(
                    userMessage = "Not '${courseName.ifBlank { "Genel" }}' dersine taşındı."
                ))
            }.onFailure { error ->
                fail("Not taşınamadı: ${error.message}", error)
            }
        }
    }

    fun moveCourseNotesToGeneral(courseName: String) {
        val normalized = courseName.trim()
        if (normalized.isBlank() || normalized.equals("Genel", ignoreCase = true)) return
        viewModelScope.launch {
            runCatching {
                _uiState.value.savedNotes
                    .filter { it.courseName.ifBlank { "Genel" } == normalized }
                    .forEach { note ->
                        container.noteRepository.updateContent(
                            id = note.id,
                            title = note.title,
                            body = note.body,
                            courseName = ""
                        )
                    }
            }.onSuccess {
                _uiState.value = withRefreshedNotes(_uiState.value.copy(
                    userMessage = "$normalized dersi silindi; notlar Genel'e taşındı."
                ))
            }.onFailure { error ->
                fail("Ders silinemedi: ${error.message}", error)
            }
        }
    }

    fun archiveNote(id: String) {
        viewModelScope.launch {
            runCatching {
                container.noteRepository.archiveNote(id)
            }.onSuccess {
                _uiState.value = withRefreshedNotes(_uiState.value.copy(
                    userMessage = "Not arşivlendi."
                ))
            }.onFailure { error ->
                fail("Not arşivlenemedi: ${error.message}", error)
            }
        }
    }

    fun unarchiveNote(id: String) {
        viewModelScope.launch {
            runCatching {
                container.noteRepository.unarchiveNote(id)
            }.onSuccess {
                _uiState.value = withRefreshedNotes(_uiState.value.copy(
                    userMessage = "Not geri yüklendi."
                ))
            }.onFailure { error ->
                fail("Not geri yüklenemedi: ${error.message}", error)
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
                    userMessage = "${files.size} debug çıktısı kaydedildi."
                )
            }.onFailure { error ->
                fail("Debug çıktıları kaydedilemedi: ${error.message}", error)
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

    fun setBackendBaseUrl(value: String) {
        viewModelScope.launch { container.settingsRepository.setBackendBaseUrl(value) }
    }

    fun setUseBackendPipeline(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setUseBackendPipeline(enabled) }
    }

    fun setShowBottomNavigation(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setShowBottomNavigation(enabled) }
    }

    fun setAllowFingerDrawing(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setAllowFingerDrawing(enabled) }
    }

    fun setUseStylusPressure(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setUseStylusPressure(enabled) }
    }

    fun setPalmRejection(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setPalmRejection(enabled) }
    }

    fun setDefaultPenWidth(value: Float) {
        viewModelScope.launch { container.settingsRepository.setDefaultPenWidth(value) }
    }

    fun setDefaultEraserSize(value: Float) {
        viewModelScope.launch { container.settingsRepository.setDefaultEraserSize(value) }
    }

    fun setCanvasMaxZoom(value: Float) {
        viewModelScope.launch {
            container.settingsRepository.setCanvasZoomRange(_uiState.value.settings.canvasMinZoom, value)
        }
    }

    fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(userMessage = message)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }

    /** Görseli backend (Qwen 2.5 VL) ile OCR'lar; sonucu döndürür. Hata fırlatabilir. */
    suspend fun ocrImageToText(bitmap: Bitmap, backendJobId: String? = null): String =
        container.backendClient.ocrImageWithQwen(bitmap, _uiState.value.settings.backendBaseUrl, backendJobId)

    /** Birleştirilmiş OCR metnini backend (Gemma) ile açıklatır; metni döndürür. Hata fırlatabilir. */
    suspend fun explainCurrentNote(text: String, title: String?): String =
        container.backendClient.explainNote(text, _uiState.value.settings.backendBaseUrl, title)

    private fun buildDebugArtifacts(rawText: String, note: FormattedNote?) =
        buildList {
            _uiState.value.boardDetection?.debugArtifacts?.let { addAll(it) }
            _uiState.value.enhancement?.debugArtifacts?.let { addAll(it) }
            add(com.board2notes.app.domain.model.DebugArtifact("ocr_raw_text", text = rawText))
            note?.let { add(com.board2notes.app.domain.model.DebugArtifact("formatted_note", text = "${it.title}\n\n${it.body}")) }
        }

    private suspend fun applyBackendResultToNote(
        noteId: String,
        noteType: NoteType,
        result: BackendPipelineResult
    ): AppliedResult {
        val current = container.noteRepository.getNote(noteId) ?: error("Hedef not bulunamadı.")
        return if (noteType == NoteType.Canvas) {
            val raw = result.whiteCanvasBitmap ?: result.ocrBitmap ?: result.cropBitmap
                ?: error("Backend canvas çıktısı üretmedi.")
            val darkened = withContext(Dispatchers.Default) {
                InkDarkener.darken(raw, _uiState.value.settings.inkDarkness)
            }
            val asset = "img_${UUID.randomUUID()}.png"
            withContext(Dispatchers.IO) { writeBitmapAsset(noteId, asset, darkened) }
            // Not gövdesi değişmez; görsel editörde merkezi/seçili ImageElement olarak eklenir.
            AppliedResult(current, asset, result.jobId)
        } else {
            val text = result.body.ifBlank { result.ocrText }.trim()
            val mergedBody = buildString {
                if (current.body.isNotBlank()) {
                    append(current.body.trimEnd())
                    append("\n\n")
                }
                append(text.ifBlank { "OCR sonucu boş döndü." })
            }
            val title = if (current.title.startsWith("Yeni ") && result.title.isNotBlank()) {
                result.title
            } else {
                current.title
            }
            val updated = container.noteRepository.updateContent(
                id = noteId,
                title = title,
                body = mergedBody,
                courseName = current.courseName
            ) ?: error("Yazı notu güncellenemedi.")
            AppliedResult(updated, null)
        }
    }

    private fun writeBitmapAsset(noteId: String, asset: String, bitmap: Bitmap) {
        val dir = File(getApplication<Application>().filesDir, "notes/$noteId").apply { mkdirs() }
        File(dir, asset).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    fun clearPendingCanvasImage() {
        if (
            _uiState.value.pendingCanvasImageAsset != null ||
            _uiState.value.pendingCanvasImagePageIndex != null ||
            _uiState.value.pendingCanvasImageJobId != null
        ) {
            _uiState.value = _uiState.value.copy(
                pendingCanvasImageAsset = null,
                pendingCanvasImagePageIndex = null,
                pendingCanvasImageJobId = null
            )
        }
    }

    fun cancelPendingScan() {
        pendingScanJob?.cancel()
        pendingScanJob = null
        _uiState.value = _uiState.value.copy(
            pendingScanNoteId = null,
            pendingScanNoteType = null,
            pendingScanPageIndex = null,
            boardDetection = null,
            manualQuad = null,
            isBusy = false,
            loadingMessage = null
        )
    }

    fun setInkDarkness(value: Float) {
        viewModelScope.launch { container.settingsRepository.setInkDarkness(value) }
    }

    private suspend fun loadNotesSafely(): List<SavedNote> =
        runCatching { container.noteRepository.listNotes() }.getOrDefault(emptyList())

    private suspend fun loadArchivedNotesSafely(): List<SavedNote> =
        runCatching { container.noteRepository.listArchivedNotes() }.getOrDefault(emptyList())

    private suspend fun loadDeletedNotesSafely(): List<SavedNote> =
        runCatching { container.noteRepository.listDeletedNotes() }.getOrDefault(emptyList())

    private suspend fun loadFavoriteNotesSafely(): List<SavedNote> =
        runCatching { container.noteRepository.listFavoriteNotes() }.getOrDefault(emptyList())

    private suspend fun withRefreshedNotes(state: Board2NotesUiState = _uiState.value): Board2NotesUiState =
        state.copy(
            savedNotes = loadNotesSafely(),
            archivedNotes = loadArchivedNotesSafely(),
            deletedNotes = loadDeletedNotesSafely(),
            favoriteNotes = loadFavoriteNotesSafely()
        )

    fun toggleFavorite(noteId: String) {
        viewModelScope.launch {
            container.noteRepository.toggleFavorite(noteId)
            _uiState.value = withRefreshedNotes()
        }
    }

    fun restoreNote(noteId: String) {
        viewModelScope.launch {
            container.noteRepository.restoreNote(noteId)
            _uiState.value = withRefreshedNotes(_uiState.value.copy(userMessage = "Not geri yüklendi."))
        }
    }

    fun deleteNotePermanently(noteId: String) {
        viewModelScope.launch {
            container.noteRepository.deleteNotePermanently(noteId)
            _uiState.value = withRefreshedNotes(_uiState.value.copy(userMessage = "Not kalıcı olarak silindi."))
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            container.noteRepository.emptyTrash()
            _uiState.value = withRefreshedNotes(_uiState.value.copy(userMessage = "Çöp kutusu boşaltıldı."))
        }
    }

    fun setUseDarkTheme(value: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setUseDarkTheme(value)
        }
    }

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
