package com.board2notes.app.data.image

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MathOpsTest {
    @Test
    fun sigmoidMapsZeroToHalf() {
        assertEquals(0.5f, MathOps.sigmoid(0f), 0.0001f)
    }

    @Test
    fun thresholdReturnsBooleanMask() {
        val result = MathOps.threshold(floatArrayOf(0.2f, 0.5f, 0.8f), 0.5f)

        assertArrayEquals(booleanArrayOf(false, true, true), result)
    }
}
