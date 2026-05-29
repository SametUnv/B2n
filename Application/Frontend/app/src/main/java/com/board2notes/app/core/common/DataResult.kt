package com.board2notes.app.core.common

/**
 * Lightweight result wrapper used between data and presentation layers.
 * When the real FastAPI backend is added, repositories can return Error
 * states without changing the ViewModel signatures.
 */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Error(val message: String, val throwable: Throwable? = null) : DataResult<Nothing>
}
