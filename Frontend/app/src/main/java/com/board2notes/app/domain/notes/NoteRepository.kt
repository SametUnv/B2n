package com.board2notes.app.domain.notes

import android.graphics.Bitmap
import com.board2notes.app.domain.model.FormattedNote
import com.board2notes.app.domain.model.OcrResult
import com.board2notes.app.domain.model.SavedNote

interface NoteRepository {
    suspend fun listNotes(): List<SavedNote>
    suspend fun listArchivedNotes(): List<SavedNote>
    suspend fun getNote(id: String): SavedNote?
    suspend fun createFromPipeline(
        note: FormattedNote,
        ocrResult: OcrResult,
        cropBitmap: Bitmap?,
        ocrBitmap: Bitmap?,
        whitePageBitmap: Bitmap?
    ): SavedNote
    suspend fun updateContent(id: String, title: String, body: String, courseName: String): SavedNote?
    suspend fun archiveNote(id: String): SavedNote?
    suspend fun unarchiveNote(id: String): SavedNote?
    suspend fun deleteNote(id: String)
}
