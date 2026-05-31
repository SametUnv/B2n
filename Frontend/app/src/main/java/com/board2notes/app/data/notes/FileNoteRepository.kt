package com.board2notes.app.data.notes

import android.graphics.Bitmap
import com.board2notes.app.data.image.CanvasNoteComposer
import com.board2notes.app.domain.model.FormattedNote
import com.board2notes.app.domain.model.NoteType
import com.board2notes.app.domain.model.OcrResult
import com.board2notes.app.domain.model.SavedNote
import com.board2notes.app.domain.notes.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class FileNoteRepository(filesDir: File) : NoteRepository {
    private val notesRoot = File(filesDir, "notes")
    private val indexFile = File(notesRoot, "notes.json")
    private val mutex = Mutex()

    override suspend fun listNotes(): List<SavedNote> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readIndex().filter { !it.isArchived && !it.isDeleted }.sortedByDescending { it.updatedAtEpochMs }
        }
    }

    override suspend fun listArchivedNotes(): List<SavedNote> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readIndex().filter { it.isArchived && !it.isDeleted }.sortedByDescending { it.updatedAtEpochMs }
        }
    }

    override suspend fun getNote(id: String): SavedNote? = withContext(Dispatchers.IO) {
        mutex.withLock {
            readIndex().firstOrNull { it.id == id }
        }
    }

    override suspend fun createBlank(noteType: NoteType): SavedNote = withContext(Dispatchers.IO) {
        mutex.withLock {
            notesRoot.mkdirs()
            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            val noteDir = File(notesRoot, id).apply { mkdirs() }
            val canvasPath = if (noteType == NoteType.Canvas) {
                writeBitmap(CanvasNoteComposer.createBlankCanvas(), File(noteDir, "canvas.png"))
            } else {
                null
            }
            val saved = SavedNote(
                id = id,
                title = if (noteType == NoteType.Canvas) "Yeni Canvas Notu" else "Yeni Yazı Notu",
                body = "",
                courseName = "",
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                ocrText = "",
                confidence = null,
                cropImagePath = null,
                ocrImagePath = null,
                whitePageImagePath = null,
                noteType = noteType,
                canvasImagePath = canvasPath
            )
            writeIndex(readIndex().filterNot { it.id == id } + saved)
            saved
        }
    }

    override suspend fun createFromPipeline(
        note: FormattedNote,
        ocrResult: OcrResult,
        cropBitmap: Bitmap?,
        ocrBitmap: Bitmap?,
        whitePageBitmap: Bitmap?,
        noteType: NoteType,
        canvasBitmap: Bitmap?
    ): SavedNote = withContext(Dispatchers.IO) {
        mutex.withLock {
            notesRoot.mkdirs()
            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            val noteDir = File(notesRoot, id).apply { mkdirs() }
            val whitePagePath = whitePageBitmap?.let { writeBitmap(it, File(noteDir, "white_page.png")) }
            val canvasPath = when {
                canvasBitmap != null -> writeBitmap(canvasBitmap, File(noteDir, "canvas.png"))
                noteType == NoteType.Canvas && whitePageBitmap != null -> writeBitmap(
                    CanvasNoteComposer.composeInkOnCanvas(
                        baseBitmap = CanvasNoteComposer.createBlankCanvas(),
                        inkSource = whitePageBitmap
                    ),
                    File(noteDir, "canvas.png")
                )
                noteType == NoteType.Canvas -> writeBitmap(CanvasNoteComposer.createBlankCanvas(), File(noteDir, "canvas.png"))
                else -> null
            }
            val saved = SavedNote(
                id = id,
                title = note.title.ifBlank { "B2Note Notu" },
                body = note.body,
                courseName = note.courseName,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                ocrText = ocrResult.rawText,
                confidence = ocrResult.confidence,
                cropImagePath = cropBitmap?.let { writeBitmap(it, File(noteDir, "crop.png")) },
                ocrImagePath = ocrBitmap?.let { writeBitmap(it, File(noteDir, "ocr.png")) },
                whitePageImagePath = whitePagePath,
                noteType = noteType,
                canvasImagePath = canvasPath
            )
            writeIndex(readIndex().filterNot { it.id == id } + saved)
            saved
        }
    }

    override suspend fun updateContent(id: String, title: String, body: String, courseName: String): SavedNote? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val notes = readIndex()
                val current = notes.firstOrNull { it.id == id } ?: return@withLock null
                val updated = current.copy(
                    title = title.ifBlank { "B2Note Notu" },
                    body = body,
                    courseName = courseName,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
                writeIndex(notes.map { if (it.id == id) updated else it })
                updated
            }
        }

    override suspend fun updateCanvas(id: String, canvasBitmap: Bitmap, pageIndex: Int): SavedNote? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val notes = readIndex()
            val current = notes.firstOrNull { it.id == id } ?: return@withLock null
            val noteDir = File(notesRoot, id).apply { mkdirs() }
            val filename = if (pageIndex == 0) "canvas.png" else "canvas_page_${pageIndex}.png"
            val filePath = writeBitmap(canvasBitmap, File(noteDir, filename))
            val updated = current.copy(
                noteType = NoteType.Canvas,
                canvasImagePath = if (pageIndex == 0) filePath else current.canvasImagePath,
                updatedAtEpochMs = System.currentTimeMillis()
            )
            writeIndex(notes.map { if (it.id == id) updated else it })
            updated
        }
    }

    override suspend fun deleteNote(id: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val notes = readIndex()
            val current = notes.firstOrNull { it.id == id } ?: return@withLock
            val updated = current.copy(isDeleted = true, updatedAtEpochMs = System.currentTimeMillis())
            writeIndex(notes.map { if (it.id == id) updated else it })
        }
    }

    override suspend fun archiveNote(id: String): SavedNote? = setArchived(id, archived = true)

    override suspend fun unarchiveNote(id: String): SavedNote? = setArchived(id, archived = false)

    private suspend fun setArchived(id: String, archived: Boolean): SavedNote? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val notes = readIndex()
            val current = notes.firstOrNull { it.id == id } ?: return@withLock null
            val updated = current.copy(
                isArchived = archived,
                updatedAtEpochMs = System.currentTimeMillis()
            )
            writeIndex(notes.map { if (it.id == id) updated else it })
            updated
        }
    }

    private fun writeBitmap(bitmap: Bitmap, file: File): String {
        file.parentFile?.mkdirs()
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return file.absolutePath
    }

    private fun readIndex(): List<SavedNote> {
        if (!indexFile.exists()) return emptyList()
        val text = indexFile.readText()
        if (text.isBlank()) return emptyList()
        val array = JSONArray(text)
        return List(array.length()) { index ->
            array.getJSONObject(index).toSavedNote()
        }
    }

    private fun writeIndex(notes: List<SavedNote>) {
        notesRoot.mkdirs()
        val array = JSONArray()
        notes.forEach { array.put(it.toJson()) }
        indexFile.writeText(array.toString(2))
    }

    private fun SavedNote.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("body", body)
        .put("courseName", courseName)
        .put("createdAtEpochMs", createdAtEpochMs)
        .put("updatedAtEpochMs", updatedAtEpochMs)
        .put("ocrText", ocrText)
        .put("confidence", confidence ?: JSONObject.NULL)
        .put("cropImagePath", cropImagePath ?: JSONObject.NULL)
        .put("ocrImagePath", ocrImagePath ?: JSONObject.NULL)
        .put("whitePageImagePath", whitePageImagePath ?: JSONObject.NULL)
        .put("noteType", noteType.name)
        .put("canvasImagePath", canvasImagePath ?: JSONObject.NULL)
        .put("isArchived", isArchived)
        .put("isFavorite", isFavorite)
        .put("isDeleted", isDeleted)

    private fun JSONObject.toSavedNote(): SavedNote = SavedNote(
        id = getString("id"),
        title = optString("title", "B2Note Notu"),
        body = optString("body"),
        courseName = optString("courseName"),
        createdAtEpochMs = optLong("createdAtEpochMs"),
        updatedAtEpochMs = optLong("updatedAtEpochMs"),
        ocrText = optString("ocrText"),
        confidence = if (isNull("confidence")) null else optDouble("confidence").toFloat(),
        cropImagePath = nullableString("cropImagePath"),
        ocrImagePath = nullableString("ocrImagePath"),
        whitePageImagePath = nullableString("whitePageImagePath"),
        noteType = optString("noteType").let { raw ->
            runCatching { NoteType.valueOf(raw) }.getOrDefault(NoteType.Text)
        },
        canvasImagePath = nullableString("canvasImagePath"),
        isArchived = optBoolean("isArchived", false),
        isFavorite = optBoolean("isFavorite", false),
        isDeleted = optBoolean("isDeleted", false)
    )

    private fun JSONObject.nullableString(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    override suspend fun listDeletedNotes(): List<SavedNote> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readIndex().filter { it.isDeleted }.sortedByDescending { it.updatedAtEpochMs }
        }
    }

    override suspend fun listFavoriteNotes(): List<SavedNote> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readIndex().filter { it.isFavorite && !it.isDeleted && !it.isArchived }.sortedByDescending { it.updatedAtEpochMs }
        }
    }

    override suspend fun toggleFavorite(id: String): SavedNote? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val notes = readIndex()
            val current = notes.firstOrNull { it.id == id } ?: return@withLock null
            val updated = current.copy(isFavorite = !current.isFavorite, updatedAtEpochMs = System.currentTimeMillis())
            writeIndex(notes.map { if (it.id == id) updated else it })
            updated
        }
    }

    override suspend fun restoreNote(id: String): SavedNote? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val notes = readIndex()
            val current = notes.firstOrNull { it.id == id } ?: return@withLock null
            val updated = current.copy(isDeleted = false, updatedAtEpochMs = System.currentTimeMillis())
            writeIndex(notes.map { if (it.id == id) updated else it })
            updated
        }
    }

    override suspend fun deleteNotePermanently(id: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            writeIndex(readIndex().filterNot { it.id == id })
            File(notesRoot, id).deleteRecursively()
        }
    }

    override suspend fun emptyTrash(): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val notes = readIndex()
            val deletedNotes = notes.filter { it.isDeleted }
            deletedNotes.forEach {
                File(notesRoot, it.id).deleteRecursively()
            }
            writeIndex(notes.filterNot { it.isDeleted })
        }
    }
}
