package com.hooked.core.domain

import com.hooked.core.logging.Logger

sealed class UseCaseResult<out T> {
    data class Success<T>(val data: T) : UseCaseResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null, val context: String? = null) : UseCaseResult<Nothing>() {
        init {
            val contextInfo = context?.let { " [$it]" } ?: ""
            val logMessage = "Use case error$contextInfo: $message"
            
            if (throwable != null) {
                Logger.error("UseCaseResult", logMessage, throwable)
            } else {
                Logger.error("UseCaseResult", logMessage)
            }
        }
    }
}

inline fun <T, R> UseCaseResult<T>.flatMap(transform: (T) -> UseCaseResult<R>): UseCaseResult<R> {
    return when (this) {
        is UseCaseResult.Success -> transform(data)
        is UseCaseResult.Error -> this
    }
}