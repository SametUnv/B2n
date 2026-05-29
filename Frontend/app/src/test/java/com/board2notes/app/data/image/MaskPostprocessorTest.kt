package com.board2notes.app.data.image

import com.board2notes.app.domain.model.BooleanMask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskPostprocessorTest {
    @Test
    fun largestComponentKeepsOnlyTheDominantRegion() {
        val data = BooleanArray(6 * 5)
        data[0] = true
        set(data, 6, 1, 1)
        set(data, 6, 2, 1)
        set(data, 6, 1, 2)
        set(data, 6, 2, 2)
        set(data, 6, 3, 2)
        val mask = BooleanMask(6, 5, data)

        val largest = MaskPostprocessor.largestComponent(mask)

        assertFalse(largest.data[0])
        assertEquals(5, largest.countTrue())
        assertTrue(largest[2, 2])
    }

    @Test
    fun boundingBoxWrapsTruePixels() {
        val data = BooleanArray(8 * 6)
        set(data, 8, 2, 1)
        set(data, 8, 5, 4)
        val box = MaskPostprocessor.boundingBox(BooleanMask(8, 6, data))

        assertEquals(2, box.left)
        assertEquals(1, box.top)
        assertEquals(6, box.right)
        assertEquals(5, box.bottom)
    }

    @Test
    fun quadDetectorFindsSkewedSegmentationCorners() {
        val width = 12
        val height = 10
        val data = BooleanArray(width * height)
        val polygon = listOf(2 to 1, 9 to 2, 8 to 8, 1 to 7)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (insidePolygon(x + 0.5f, y + 0.5f, polygon)) {
                    set(data, width, x, y)
                }
            }
        }

        val result = MaskQuadDetector.detect(BooleanMask(width, height, data))

        requireNotNull(result)
        assertEquals("convex_hull_extremes", result.strategy)
        assertTrue(result.quad.topLeft.x <= 3f)
        assertTrue(result.quad.topRight.x >= 7f)
        assertTrue(result.quad.bottomRight.y >= 6f)
        assertTrue(result.quad.bottomLeft.x <= 2f)
    }

    private fun set(data: BooleanArray, width: Int, x: Int, y: Int) {
        data[y * width + x] = true
    }

    private fun insidePolygon(x: Float, y: Float, polygon: List<Pair<Int, Int>>): Boolean {
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val xi = polygon[i].first.toFloat()
            val yi = polygon[i].second.toFloat()
            val xj = polygon[j].first.toFloat()
            val yj = polygon[j].second.toFloat()
            val intersect = (yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}
