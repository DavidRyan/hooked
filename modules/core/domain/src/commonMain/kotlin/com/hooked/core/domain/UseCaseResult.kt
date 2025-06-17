package com.hooked.core.domain

import com.hooked.core.logging.Logger

/**
 * Base sealed class for use case results with automatic error logging
 */
sealed class UseCaseResult<out T> {
    data class Success<T>(val data: T) : UseCaseResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null, val context: String? = null) : UseCaseResult<Nothing>() {
        init {
            // Automatically log errors when they are created
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

// Helper functions for creating UseCaseResult with automatic logging
object UseCaseResultFactory {
    fun <T> success(data: T): UseCaseResult<T> = UseCaseResult.Success(data)
    
    fun error(message: String, context: String? = null): UseCaseResult<Nothing> = 
        UseCaseResult.Error(message, null, context)
    
    fun error(message: String, throwable: Throwable, context: String? = null): UseCaseResult<Nothing> = 
        UseCaseResult.Error(message, throwable, context)
    
    fun error(throwable: Throwable, context: String? = null): UseCaseResult<Nothing> = 
        UseCaseResult.Error(throwable.message ?: "Unknown error", throwable, context)
}

// Extension functions for easier error handling with logging
inline fun <T> UseCaseResult<T>.onError(action: (UseCaseResult.Error) -> Unit): UseCaseResult<T> {
    if (this is UseCaseResult.Error) {
        action(this)
    }
    return this
}

inline fun <T> UseCaseResult<T>.onSuccess(action: (T) -> Unit): UseCaseResult<T> {
    if (this is UseCaseResult.Success) {
        action(data)
    }
    return this
}