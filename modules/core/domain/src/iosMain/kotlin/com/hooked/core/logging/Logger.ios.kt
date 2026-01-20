package com.hooked.core.logging

import platform.Foundation.NSLog

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
            NSLog(formatMessage(LogLevel.VERBOSE, tag, message))
        }
    }
    
    actual fun verbose(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.VERBOSE)) {
            NSLog(formatMessageWithThrowable(LogLevel.VERBOSE, tag, message, throwable))
        }
    }
    
    // Trace
    actual fun trace(tag: String, message: String) {
        if (shouldLog(LogLevel.TRACE)) {
            NSLog(formatMessage(LogLevel.TRACE, tag, message))
        }
    }
    
    actual fun trace(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.TRACE)) {
            NSLog(formatMessageWithThrowable(LogLevel.TRACE, tag, message, throwable))
        }
    }
    
    // Debug
    actual fun debug(tag: String, message: String) {
        if (shouldLog(LogLevel.DEBUG)) {
            NSLog(formatMessage(LogLevel.DEBUG, tag, message))
        }
    }
    
    actual fun debug(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.DEBUG)) {
            NSLog(formatMessageWithThrowable(LogLevel.DEBUG, tag, message, throwable))
        }
    }
    
    // Info
    actual fun info(tag: String, message: String) {
        if (shouldLog(LogLevel.INFO)) {
            NSLog(formatMessage(LogLevel.INFO, tag, message))
        }
    }
    
    actual fun info(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.INFO)) {
            NSLog(formatMessageWithThrowable(LogLevel.INFO, tag, message, throwable))
        }
    }
    
    // Warning
    actual fun warning(tag: String, message: String) {
        if (shouldLog(LogLevel.WARNING)) {
            NSLog(formatMessage(LogLevel.WARNING, tag, message))
        }
    }
    
    actual fun warning(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.WARNING)) {
            NSLog(formatMessageWithThrowable(LogLevel.WARNING, tag, message, throwable))
        }
    }
    
    // Error
    actual fun error(tag: String, message: String) {
        if (shouldLog(LogLevel.ERROR)) {
            NSLog(formatMessage(LogLevel.ERROR, tag, message))
        }
    }
    
    actual fun error(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.ERROR)) {
            NSLog(formatMessageWithThrowable(LogLevel.ERROR, tag, message, throwable))
        }
    }
}
