package com.board2notes.app.data.backend

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class BackendPipelineResult(
    val jobId: String,
    val title: String,
    val body: String,
    val ocrText: String,
    val whiteCanvasBitmap: Bitmap?,
    val ocrBitmap: Bitmap?,
    val cropBitmap: Bitmap?,
    val overlayBitmap: Bitmap?,
    val warnings: List<String>,
    val timingsMs: Map<String, Double>
)

class BackendBoard2NotesClient {
    suspend fun runPipeline(
        bitmap: Bitmap,
        baseUrl: String,
        threshold: Float,
        runOcr: Boolean
    ): BackendPipelineResult = withContext(Dispatchers.IO) {
        val root = normalizeBaseUrl(baseUrl)
        val response = postPipeline(root, bitmap, threshold, runOcr)
        val artifacts = response.optJSONObject("artifacts") ?: JSONObject()
        val note = response.optJSONObject("note") ?: JSONObject()
        BackendPipelineResult(
            jobId = response.optString("job_id"),
            title = note.optString("title", "B2Note Notu"),
            body = note.optString("body"),
            ocrText = response.optString("ocr_text"),
            whiteCanvasBitmap = fetchBitmap(root, artifacts.optString("white_canvas")),
            ocrBitmap = fetchBitmap(root, artifacts.optString("ocr_enhanced")),
            cropBitmap = fetchBitmap(root, artifacts.optString("perspective_crop")),
            overlayBitmap = fetchBitmap(root, artifacts.optString("overlay")),
            warnings = response.optJSONArray("warnings").toStringList(),
            timingsMs = response.optJSONObject("timings_ms").toDoubleMap()
        )
    }

    internal fun normalizeBaseUrl(baseUrl: String): String {
        val root = baseUrl.trim().trimEnd('/')
        require(root.startsWith("http://") || root.startsWith("https://")) {
            "Backend URL http:// veya https:// ile başlamalı."
        }
        return root
    }

    internal fun artifactUrl(baseUrl: String, path: String): String {
        return if (path.startsWith("http://") || path.startsWith("https://")) path else "${normalizeBaseUrl(baseUrl)}$path"
    }

    private fun postPipeline(root: String, bitmap: Bitmap, threshold: Float, runOcr: Boolean): JSONObject {
        val boundary = "B2N-${UUID.randomUUID()}"
        val connection = (URL("$root/api/v1/pipeline").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 180_000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        connection.outputStream.buffered().use { output ->
            fun write(text: String) = output.write(text.toByteArray(Charsets.UTF_8))
            fun field(name: String, value: String) {
                write("--$boundary\r\n")
                write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                write(value)
                write("\r\n")
            }
            field("threshold", threshold.toString())
            field("run_ocr", runOcr.toString())
            write("--$boundary\r\n")
            write("Content-Disposition: form-data; name=\"image\"; filename=\"board.jpg\"\r\n")
            write("Content-Type: image/jpeg\r\n\r\n")
            output.write(bitmap.toJpegBytes())
            write("\r\n--$boundary--\r\n")
        }

        val status = connection.responseCode
        val body = if (status in 200..299) {
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        }
        connection.disconnect()
        if (status !in 200..299) {
            throw IllegalStateException("Backend pipeline HTTP $status: $body")
        }
        return JSONObject(body)
    }

    private fun fetchBitmap(root: String, path: String): Bitmap? {
        if (path.isBlank()) return null
        val connection = (URL(artifactUrl(root, path)).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 60_000
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            BitmapFactory.decodeStream(connection.inputStream)
        } finally {
            connection.disconnect()
        }
    }

    private fun Bitmap.toJpegBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 94, output)
        return output.toByteArray()
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
    }

    private fun JSONObject?.toDoubleMap(): Map<String, Double> {
        if (this == null) return emptyMap()
        return keys().asSequence().associateWith { key -> optDouble(key) }
    }
}
