package com.board2notes.app.data.notes

import com.board2notes.app.domain.model.FormattedNote
import com.board2notes.app.domain.model.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileNoteRepositoryTest {
    @Test
    fun repositoryCreatesUpdatesListsAndDeletesNotes() {
        val root = createTempRoot()
        try {
            val repository = FileNoteRepository(root)
            val saved = kotlinx.coroutines.runBlocking {
                repository.createFromPipeline(
                    note = FormattedNote(
                        title = "Limit",
                        body = "Limit kurallari",
                        courseName = "Matematik",
                        createdAtEpochMs = 1L
                    ),
                    ocrResult = OcrResult(
                        rawText = "Limit kurallari",
                        lines = emptyList(),
                        words = emptyList(),
                        confidence = null,
                        elapsedMs = 10L
                    ),
                    cropBitmap = null,
                    ocrBitmap = null,
                    whitePageBitmap = null
                )
            }

            kotlinx.coroutines.runBlocking {
                assertEquals(listOf(saved.id), repository.listNotes().map { it.id })
                val updated = repository.updateContent(saved.id, "Turev", "Turev kurallari", "Matematik")
                assertEquals("Turev", updated?.title)
                assertTrue(repository.listNotes().first().body.contains("Turev"))
                repository.deleteNote(saved.id)
                assertNull(repository.getNote(saved.id))
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun createTempRoot(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "b2note_notes_test_${System.nanoTime()}")
        dir.mkdirs()
        return dir
    }
}
