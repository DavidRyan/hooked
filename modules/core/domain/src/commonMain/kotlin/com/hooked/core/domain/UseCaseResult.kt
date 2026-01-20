package com.hooked.core.domain

import com.hooked.core.logging.Logger

sealed class UseCaseResult<out T> {
    data class Success<T>(val data: T) : UseCaseResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null, val context: String? = null) : UseCaseResult<Nothing>() {
        init {
            val tag = context ?: "UseCaseResult"
            if (throwable != null) {
                Logger.error(tag, message, throwable)
            } else {
                Logger.error(tag, message)
            }
        }
    }
}