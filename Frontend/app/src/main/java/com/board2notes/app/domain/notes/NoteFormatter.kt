package com.board2notes.app.domain.notes

import com.board2notes.app.domain.model.FormattedNote
import com.board2notes.app.domain.model.OcrResult

interface NoteFormatter {
    fun format(ocrResult: OcrResult): FormattedNote
}
