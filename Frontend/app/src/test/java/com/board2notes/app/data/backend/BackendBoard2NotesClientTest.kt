package com.board2notes.app.data.backend

import org.junit.Assert.assertEquals
import org.junit.Test

class BackendBoard2NotesClientTest {
    @Test
    fun artifactUrlResolvesRelativePathsAgainstBaseUrl() {
        val client = BackendBoard2NotesClient()

        assertEquals(
            "http://10.0.2.2:8000/api/v1/artifacts/job/model2_white_canvas.png",
            client.artifactUrl("http://10.0.2.2:8000/", "/api/v1/artifacts/job/model2_white_canvas.png")
        )
    }

    @Test
    fun artifactUrlKeepsAbsolutePaths() {
        val client = BackendBoard2NotesClient()

        assertEquals(
            "https://example.com/file.png",
            client.artifactUrl("http://10.0.2.2:8000", "https://example.com/file.png")
        )
    }
}
