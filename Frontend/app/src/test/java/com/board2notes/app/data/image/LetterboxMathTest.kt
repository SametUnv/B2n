package com.board2notes.app.data.image

import org.junit.Assert.assertEquals
import org.junit.Test

class LetterboxMathTest {
    @Test
    fun wideImageKeepsAspectAndPadsVertically() {
        val meta = LetterboxMath.compute(originalWidth = 1280, originalHeight = 720, targetSize = 640)

        assertEquals(640, meta.resizedWidth)
        assertEquals(360, meta.resizedHeight)
        assertEquals(0, meta.padX)
        assertEquals(140, meta.padY)
        assertEquals(0.5f, meta.scale, 0.0001f)
    }

    @Test
    fun tallImageKeepsAspectAndPadsHorizontally() {
        val meta = LetterboxMath.compute(originalWidth = 720, originalHeight = 1280, targetSize = 640)

        assertEquals(360, meta.resizedWidth)
        assertEquals(640, meta.resizedHeight)
        assertEquals(140, meta.padX)
        assertEquals(0, meta.padY)
        assertEquals(0.5f, meta.scale, 0.0001f)
    }
}
