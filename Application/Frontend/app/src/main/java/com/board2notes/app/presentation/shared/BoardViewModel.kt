package com.board2notes.app.presentation.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.board2notes.app.core.common.DataResult
import com.board2notes.app.domain.model.BoardMode
import com.board2notes.app.domain.usecase.BoardUseCases
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One ViewModel shared by every screen in the flow (scoped to the nav graph).
 * Holds [BoardUiState] and orchestrates the use cases.
 */
class BoardViewModel(
    private val useCases: BoardUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardUiState())
    val uiState: StateFlow<BoardUiState> = _uiState.asStateFlow()

    // --- Image selection -----------------------------------------------------

    fun onImageSelected(uri: String) {
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                originalImageUrl = uri,
                enhancedImageUrl = null,
                errorMessage = null
            )
        }
    }

    fun clearImage() {
        _uiState.update { it.copy(selectedImageUri = null, originalImageUrl = null, enhancedImageUrl = null) }
    }

    // --- Processing (enhancement) -------------------------------------------

    /** Runs the staged loading + enhancement. Calls [onDone] when finished. */
    fun startProcessing(onDone: () -> Unit) {
        val uri = _uiState.value.selectedImageUri ?: run {
            _uiState.update { it.copy(errorMessage = "Önce bir görsel seçin.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingStepIndex = 0, errorMessage = null) }

            // Animate through the visible steps
            for (step in ProcessingSteps.indices) {
                _uiState.update { it.copy(processingStepIndex = step) }
                delay(650)
            }

            when (val result = useCases.enhanceImage(uri)) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            enhancedImageUrl = result.data.enhancedImageUri
                        )
                    }
                    onDone()
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isProcessing = false, errorMessage = result.message) }
                }
            }
        }
    }

    // --- Mode selection ------------------------------------------------------

    fun selectMode(mode: BoardMode, onReady: () -> Unit) {
        val uri = _uiState.value.selectedImageUri ?: return
        _uiState.update { it.copy(selectedMode = mode) }
        viewModelScope.launch {
            when (mode) {
                BoardMode.OCR_NOTE -> {
                    when (val r = useCases.generateOcrNote(uri)) {
                        is DataResult.Success -> _uiState.update { it.copy(mockOcrText = r.data.text) }
                        is DataResult.Error -> _uiState.update { it.copy(errorMessage = r.message) }
                    }
                }
                BoardMode.VISUAL_NOTE -> {
                    when (val r = useCases.generateVisualNote(uri)) {
                        is DataResult.Success -> _uiState.update {
                            it.copy(
                                visualNoteTitle = r.data.title,
                                visualNoteLines = r.data.lines
                            )
                        }
                        is DataResult.Error -> _uiState.update { it.copy(errorMessage = r.message) }
                    }
                }
            }
            onReady()
        }
    }

    fun updateOcrText(newText: String) {
        _uiState.update { it.copy(mockOcrText = newText) }
    }

    // --- Export --------------------------------------------------------------

    fun exportCurrentNote(onExported: () -> Unit) {
        val state = _uiState.value
        val mode = state.selectedMode ?: return
        val content = if (mode == BoardMode.OCR_NOTE) state.mockOcrText
        else state.visualNoteLines.joinToString("\n")
        viewModelScope.launch {
            when (val r = useCases.exportNote(content, mode)) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(exportFileName = r.data.fileName, exportDate = r.data.createdAt)
                    }
                    onExported()
                }
                is DataResult.Error -> _uiState.update { it.copy(errorMessage = r.message) }
            }
        }
    }

    // --- Reset (back to Home for a new capture) ------------------------------

    fun resetFlow() {
        _uiState.value = BoardUiState()
    }

    /** Factory so the VM can receive its use cases (manual DI, no Hilt). */
    class Factory(private val useCases: BoardUseCases) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BoardViewModel(useCases) as T
        }
    }
}
