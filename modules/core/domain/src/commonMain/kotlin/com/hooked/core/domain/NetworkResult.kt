package com.hooked.core.domain

import com.hooked.core.logging.Logger

sealed class NetworkResult<out T> {
    object Loading : NetworkResult<Nothing>()
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val error: Throwable, val context: String? = null) : NetworkResult<Nothing>() {
        init {
            // Automatically log errors when they are created
            val tag = context ?: "NetworkResult"
            Logger.error(tag, "← ERROR | ${error.message}", error)
        }
    }
}

// Helper functions for creating NetworkResult with automatic logging
object NetworkResultFactory {
    fun <T> success(data: T): NetworkResult<T> = NetworkResult.Success(data)
    
    fun error(throwable: Throwable, context: String? = null): NetworkResult<Nothing> = 
        NetworkResult.Error(throwable, context)
    
    fun error(message: String, context: String? = null): NetworkResult<Nothing> = 
        NetworkResult.Error(Exception(message), context)
}
