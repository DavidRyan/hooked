package com.hooked.core.logging

/**
 * Log levels in order of verbosity (most verbose to least verbose)
 */
enum class LogLevel {
    VERBOSE,
    TRACE,
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    NONE  // Disables all logging
}

/**
 * Configuration for the Logger
 */
data class LoggerConfig(
    val isEnabled: Boolean = true,
    val minLogLevel: LogLevel = LogLevel.DEBUG,
    val appTag: String = "Hooked"
)

/**
 * Cross-platform Logger with structured output formatting.
 * 
 * Output format: [AppTag] [LEVEL] [tag] message
 * Example: [Hooked] [DEBUG] [CatchApiService] → GET /user_catches
 * 
 * Configure in your Application class:
 * ```
 * Logger.configure(
 *     isEnabled = true,
 *     minLogLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.ERROR,
 *     appTag = "Hooked"
 * )
 * ```
 */
expect object Logger {
    /**
     * Current logger configuration
     */
    var config: LoggerConfig
    
    /**
     * Configure the logger
     */
    fun configure(
        isEnabled: Boolean = true,
        minLogLevel: LogLevel = LogLevel.DEBUG,
        appTag: String = "Hooked"
    )
    
    // Standard log levels
    fun verbose(tag: String, message: String)
    fun verbose(tag: String, message: String, throwable: Throwable)
    
    fun trace(tag: String, message: String)
    fun trace(tag: String, message: String, throwable: Throwable)
    
    fun debug(tag: String, message: String)
    fun debug(tag: String, message: String, throwable: Throwable)
    
    fun info(tag: String, message: String)
    fun info(tag: String, message: String, throwable: Throwable)
    
    fun warning(tag: String, message: String)
    fun warning(tag: String, message: String, throwable: Throwable)
    
    fun error(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable)
}

// =============================================================================
// Network Logging Helpers
// =============================================================================

/**
 * Log an outgoing network request
 * Output: → METHOD /endpoint
 */
fun Logger.logRequest(tag: String, method: String, endpoint: String) {
    debug(tag, "→ $method $endpoint")
}

/**
 * Log a successful network response
 * Output: ← STATUS_CODE STATUS_TEXT
 */
fun Logger.logResponse(tag: String, statusCode: Int, statusText: String = "OK") {
    info(tag, "← $statusCode $statusText")
}

/**
 * Log a failed network response
 * Output: ← STATUS_CODE | error message
 */
fun Logger.logResponseError(tag: String, statusCode: Int, errorMessage: String) {
    error(tag, "← $statusCode | $errorMessage")
}

/**
 * Log a network error (no response received)
 * Output: ← ERROR | error message
 */
fun Logger.logNetworkError(tag: String, errorMessage: String, throwable: Throwable? = null) {
    if (throwable != null) {
        error(tag, "← ERROR | $errorMessage", throwable)
    } else {
        error(tag, "← ERROR | $errorMessage")
    }
}
