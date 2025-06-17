package com.hooked.core.logging

expect object Logger {
    fun debug(tag: String, message: String)
    fun debug(tag: String, message: String, throwable: Throwable)
    fun info(tag: String, message: String)
    fun warning(tag: String, message: String)
    fun warning(tag: String, message: String, throwable: Throwable)
    fun error(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable)
}

// Extension functions for easier usage
inline fun <reified T> T.logDebug(message: String) {
    Logger.debug(T::class.simpleName ?: "Unknown", message)
}

inline fun <reified T> T.logDebug(message: String, throwable: Throwable) {
    Logger.debug(T::class.simpleName ?: "Unknown", message, throwable)
}

inline fun <reified T> T.logInfo(message: String) {
    Logger.info(T::class.simpleName ?: "Unknown", message)
}

inline fun <reified T> T.logWarning(message: String) {
    Logger.warning(T::class.simpleName ?: "Unknown", message)
}

inline fun <reified T> T.logWarning(message: String, throwable: Throwable) {
    Logger.warning(T::class.simpleName ?: "Unknown", message, throwable)
}

inline fun <reified T> T.logError(message: String) {
    Logger.error(T::class.simpleName ?: "Unknown", message)
}

inline fun <reified T> T.logError(message: String, throwable: Throwable) {
    Logger.error(T::class.simpleName ?: "Unknown", message, throwable)
}