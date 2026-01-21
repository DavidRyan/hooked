package com.hooked.core.config

/**
 * Platform-specific application configuration.
 * Values are loaded from environment variables:
 * - Android: from .env file via BuildConfig
 * - iOS: from Config.xcconfig
 */
expect object AppConfig {
    val MAPBOX_ACCESS_TOKEN: String
    val API_BASE_URL: String
}
