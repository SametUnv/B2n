package com.board2notes.app.data.image

import kotlin.math.roundToInt

data class LetterboxMeta(
    val originalWidth: Int,
    val originalHeight: Int,
    val targetSize: Int,
    val resizedWidth: Int,
    val resizedHeight: Int,
    val padX: Int,
    val padY: Int,
    val scale: Float
)

object LetterboxMath {
    fun compute(originalWidth: Int, originalHeight: Int, targetSize: Int): LetterboxMeta {
        require(originalWidth > 0 && originalHeight > 0) { "Image dimensions must be positive." }
        require(targetSize > 0) { "Target size must be positive." }
        val scale = minOf(targetSize.toFloat() / originalWidth, targetSize.toFloat() / originalHeight)
        val resizedWidth = (originalWidth * scale).roundToInt()
        val resizedHeight = (originalHeight * scale).roundToInt()
        return LetterboxMeta(
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            targetSize = targetSize,
            resizedWidth = resizedWidth,
            resizedHeight = resizedHeight,
            padX = (targetSize - resizedWidth) / 2,
            padY = (targetSize - resizedHeight) / 2,
            scale = scale
        )
    }
}
