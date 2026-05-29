package com.board2notes.app.domain.model

import kotlin.math.max
import kotlin.math.min

data class RectBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = max(0, right - left)
    val height: Int get() = max(0, bottom - top)
    val isValid: Boolean get() = width > 0 && height > 0

    fun padded(pad: Int, maxWidth: Int, maxHeight: Int): RectBox = RectBox(
        left = max(0, left - pad),
        top = max(0, top - pad),
        right = min(maxWidth, right + pad),
        bottom = min(maxHeight, bottom + pad)
    )
}

data class PointF2(
    val x: Float,
    val y: Float
)

data class Quad(
    val topLeft: PointF2,
    val topRight: PointF2,
    val bottomRight: PointF2,
    val bottomLeft: PointF2
) {
    fun toFloatArray(): FloatArray = floatArrayOf(
        topLeft.x, topLeft.y,
        topRight.x, topRight.y,
        bottomRight.x, bottomRight.y,
        bottomLeft.x, bottomLeft.y
    )
}

data class BooleanMask(
    val width: Int,
    val height: Int,
    val data: BooleanArray
) {
    init {
        require(data.size == width * height) { "Mask data size must match width * height." }
    }

    operator fun get(x: Int, y: Int): Boolean = data[y * width + x]

    fun countTrue(): Int = data.count { it }
}
