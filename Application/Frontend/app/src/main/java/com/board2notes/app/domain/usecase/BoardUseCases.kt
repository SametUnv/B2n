package com.board2notes.app.domain.usecase

import com.board2notes.app.domain.model.BoardMode
import com.board2notes.app.domain.repository.BoardRepository

/**
 * Use cases expose single, intention-revealing operations to the ViewModel.
 * They are thin wrappers today, but give a stable place to add validation,
 * caching or analytics once the real backend lands.
 */
class EnhanceImageUseCase(private val repository: BoardRepository) {
    suspend operator fun invoke(imageUri: String) = repository.enhanceImage(imageUri)
}

class GenerateOcrNoteUseCase(private val repository: BoardRepository) {
    suspend operator fun invoke(imageUri: String) = repository.generateOcrNote(imageUri)
}

class GenerateVisualNoteUseCase(private val repository: BoardRepository) {
    suspend operator fun invoke(imageUri: String) = repository.generateVisualNote(imageUri)
}

class ExportNoteUseCase(private val repository: BoardRepository) {
    suspend operator fun invoke(content: String, mode: BoardMode) =
        repository.exportNote(content, mode)
}

/** Convenience bundle so the ViewModel takes one dependency instead of four. */
data class BoardUseCases(
    val enhanceImage: EnhanceImageUseCase,
    val generateOcrNote: GenerateOcrNoteUseCase,
    val generateVisualNote: GenerateVisualNoteUseCase,
    val exportNote: ExportNoteUseCase
)
