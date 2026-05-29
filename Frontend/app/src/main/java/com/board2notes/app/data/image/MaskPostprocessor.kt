package com.board2notes.app.data.image

import com.board2notes.app.domain.model.BooleanMask
import com.board2notes.app.domain.model.RectBox
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object MaskPostprocessor {
    fun unletterboxProbabilities(mask640: Array<FloatArray>, meta: LetterboxMeta): FloatArray {
        val out = FloatArray(meta.originalWidth * meta.originalHeight)
        for (y in 0 until meta.originalHeight) {
            val sourceY = (meta.padY + y * meta.scale).roundToInt().coerceIn(0, meta.targetSize - 1)
            for (x in 0 until meta.originalWidth) {
                val sourceX = (meta.padX + x * meta.scale).roundToInt().coerceIn(0, meta.targetSize - 1)
                out[y * meta.originalWidth + x] = mask640[sourceY][sourceX]
            }
        }
        return out
    }

    fun cleanMask(width: Int, height: Int, probabilities: FloatArray, threshold: Float): BooleanMask {
        val thresholded = BooleanMask(width, height, MathOps.threshold(probabilities, threshold))
        val opened = dilate(erode(thresholded))
        val closed = erode(dilate(opened))
        return largestComponent(closed)
    }

    fun largestComponent(mask: BooleanMask): BooleanMask {
        val visited = BooleanArray(mask.data.size)
        val queue = IntArray(mask.data.size)
        var bestPixels = IntArray(0)
        val neighborsX = intArrayOf(1, -1, 0, 0)
        val neighborsY = intArrayOf(0, 0, 1, -1)

        for (start in mask.data.indices) {
            if (!mask.data[start] || visited[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            visited[start] = true

            while (head < tail) {
                val current = queue[head++]
                val x = current % mask.width
                val y = current / mask.width
                for (i in 0..3) {
                    val nx = x + neighborsX[i]
                    val ny = y + neighborsY[i]
                    if (nx !in 0 until mask.width || ny !in 0 until mask.height) continue
                    val ni = ny * mask.width + nx
                    if (mask.data[ni] && !visited[ni]) {
                        visited[ni] = true
                        queue[tail++] = ni
                    }
                }
            }

            if (tail > bestPixels.size) {
                bestPixels = queue.copyOf(tail)
            }
        }

        val clean = BooleanArray(mask.data.size)
        for (index in bestPixels) clean[index] = true
        return BooleanMask(mask.width, mask.height, clean)
    }

    fun boundingBox(mask: BooleanMask): RectBox {
        var minX = mask.width
        var minY = mask.height
        var maxX = -1
        var maxY = -1
        for (y in 0 until mask.height) {
            for (x in 0 until mask.width) {
                if (mask[x, y]) {
                    minX = min(minX, x)
                    minY = min(minY, y)
                    maxX = max(maxX, x)
                    maxY = max(maxY, y)
                }
            }
        }
        return if (maxX < minX || maxY < minY) {
            RectBox(0, 0, 0, 0)
        } else {
            RectBox(minX, minY, maxX + 1, maxY + 1)
        }
    }

    private fun erode(mask: BooleanMask): BooleanMask {
        val out = BooleanArray(mask.data.size)
        for (y in 0 until mask.height) {
            for (x in 0 until mask.width) {
                var keep = true
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx !in 0 until mask.width || ny !in 0 until mask.height || !mask[nx, ny]) {
                            keep = false
                        }
                    }
                }
                out[y * mask.width + x] = keep
            }
        }
        return BooleanMask(mask.width, mask.height, out)
    }

    private fun dilate(mask: BooleanMask): BooleanMask {
        val out = BooleanArray(mask.data.size)
        for (y in 0 until mask.height) {
            for (x in 0 until mask.width) {
                var keep = false
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until mask.width && ny in 0 until mask.height && mask[nx, ny]) {
                            keep = true
                        }
                    }
                }
                out[y * mask.width + x] = keep
            }
        }
        return BooleanMask(mask.width, mask.height, out)
    }
}
