package com.board2notes.app.data.notes

import com.board2notes.app.domain.model.FormattedNote
import com.board2notes.app.domain.model.OcrResult
import com.board2notes.app.domain.notes.NoteFormatter

class RuleBasedNoteFormatter : NoteFormatter {
    override fun format(ocrResult: OcrResult): FormattedNote {
        val lines = ocrResult.rawText
            .replace("\r", "\n")
            .lines()
            .map { line -> line.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotBlank() }
            .fold(mutableListOf<String>()) { acc, line ->
                if (acc.lastOrNull()?.equals(line, ignoreCase = true) != true) acc += line
                acc
            }

        val title = lines.firstOrNull()
            ?.take(70)
            ?.replace(Regex("[.:;,-]+$"), "")
            ?: "B2Note Notu"

        val body = if (lines.isEmpty()) {
            "OCR sonucu boş. Fotoğrafı daha net çekmeyi veya threshold değerini değiştirmeyi deneyin."
        } else {
            lines.joinToString(separator = "\n") { line ->
                if (looksLikeBullet(line)) line else line
            }
        }

        return FormattedNote(
            title = title,
            body = body,
            createdAtEpochMs = System.currentTimeMillis()
        )
    }

    private fun looksLikeBullet(line: String): Boolean =
        line.startsWith("-") || line.startsWith("*") || line.matches(Regex("^\\d+[.)].*"))
}
