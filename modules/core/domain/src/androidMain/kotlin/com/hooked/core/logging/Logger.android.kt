package com.hooked.core.logging

import android.util.Log

actual object Logger {
    actual var config: LoggerConfig = LoggerConfig()
    
    actual fun configure(
        isEnabled: Boolean,
        minLogLevel: LogLevel,
        appTag: String
    ) {
        config = LoggerConfig(
            isEnabled = isEnabled,
            minLogLevel = minLogLevel,
            appTag = appTag
        )
    }
    
    private fun shouldLog(level: LogLevel): Boolean {
        return config.isEnabled && level.ordinal >= config.minLogLevel.ordinal
    }
    
    private fun formatMessage(level: LogLevel, tag: String, message: String): String {
        val levelStr = level.name.padEnd(7) // Pad for alignment
        return "[${config.appTag}] [$levelStr] [$tag] $message"
    }
    
    private fun formatMessageWithThrowable(level: LogLevel, tag: String, message: String, throwable: Throwable): String {
        val levelStr = level.name.padEnd(7)
        return "[${config.appTag}] [$levelStr] [$tag] $message | ${throwable::class.simpleName}: ${throwable.message}"
    }
    
    // Verbose
    actual fun verbose(tag: String, message: String) {
        if (shouldLog(LogLevel.VERBOSE)) {
            Log.v(config.appTag, formatMessage(LogLevel.VERBOSE, tag, message))
        }
    }
    
    actual fun verbose(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.VERBOSE)) {
            Log.v(config.appTag, formatMessageWithThrowable(LogLevel.VERBOSE, tag, message, throwable), throwable)
        }
    }
    
    // Trace (maps to VERBOSE on Android since there's no trace level)
    actual fun trace(tag: String, message: String) {
        if (shouldLog(LogLevel.TRACE)) {
            Log.v(config.appTag, formatMessage(LogLevel.TRACE, tag, message))
        }
    }
    
    actual fun trace(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.TRACE)) {
            Log.v(config.appTag, formatMessageWithThrowable(LogLevel.TRACE, tag, message, throwable), throwable)
        }
    }
    
    // Debug
    actual fun debug(tag: String, message: String) {
        if (shouldLog(LogLevel.DEBUG)) {
            Log.d(config.appTag, formatMessage(LogLevel.DEBUG, tag, message))
        }
    }
    
    actual fun debug(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.DEBUG)) {
            Log.d(config.appTag, formatMessageWithThrowable(LogLevel.DEBUG, tag, message, throwable), throwable)
        }
    }
    
    // Info
    actual fun info(tag: String, message: String) {
        if (shouldLog(LogLevel.INFO)) {
            Log.i(config.appTag, formatMessage(LogLevel.INFO, tag, message))
        }
    }
    
    actual fun info(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.INFO)) {
            Log.i(config.appTag, formatMessageWithThrowable(LogLevel.INFO, tag, message, throwable), throwable)
        }
    }
    
    // Warning
    actual fun warning(tag: String, message: String) {
        if (shouldLog(LogLevel.WARNING)) {
            Log.w(config.appTag, formatMessage(LogLevel.WARNING, tag, message))
        }
    }
    
    actual fun warning(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.WARNING)) {
            Log.w(config.appTag, formatMessageWithThrowable(LogLevel.WARNING, tag, message, throwable), throwable)
        }
    }
    
    // Error
    actual fun error(tag: String, message: String) {
        if (shouldLog(LogLevel.ERROR)) {
            Log.e(config.appTag, formatMessage(LogLevel.ERROR, tag, message))
        }
    }
    
    actual fun error(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.ERROR)) {
            Log.e(config.appTag, formatMessageWithThrowable(LogLevel.ERROR, tag, message, throwable), throwable)
        }
    }
}
