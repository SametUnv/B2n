package com.board2notes.app.di

import com.board2notes.app.data.repository.MockBoardRepository
import com.board2notes.app.domain.repository.BoardRepository
import com.board2notes.app.domain.usecase.BoardUseCases
import com.board2notes.app.domain.usecase.EnhanceImageUseCase
import com.board2notes.app.domain.usecase.ExportNoteUseCase
import com.board2notes.app.domain.usecase.GenerateOcrNoteUseCase
import com.board2notes.app.domain.usecase.GenerateVisualNoteUseCase

/**
 * Minimal manual dependency container.
 *
 * This is intentionally tiny so the project has zero DI-framework setup.
 * It can be replaced by Hilt later with no impact on screens/ViewModels.
 *
 * 👉 To switch to the real backend, change ONE line below:
 *      private val repository: BoardRepository = RemoteBoardRepository(api)
 */
object AppModule {

    private val repository: BoardRepository = MockBoardRepository()

    val boardUseCases: BoardUseCases by lazy {
        BoardUseCases(
            enhanceImage = EnhanceImageUseCase(repository),
            generateOcrNote = GenerateOcrNoteUseCase(repository),
            generateVisualNote = GenerateVisualNoteUseCase(repository),
            exportNote = ExportNoteUseCase(repository)
        )
    }
}
