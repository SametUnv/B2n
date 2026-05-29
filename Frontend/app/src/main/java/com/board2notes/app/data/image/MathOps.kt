package com.board2notes.app.data.image

import kotlin.math.exp

object MathOps {
    fun sigmoid(value: Float): Float = (1.0f / (1.0f + exp(-value))).toFloat()

    fun threshold(values: FloatArray, threshold: Float): BooleanArray =
        BooleanArray(values.size) { index -> values[index] >= threshold }
}
