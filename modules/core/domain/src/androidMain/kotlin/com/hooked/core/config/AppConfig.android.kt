package com.hooked.core.config

/**
 * Android implementation - values are injected at app startup from BuildConfig
 */
actual object AppConfig {
    // These are set by the app module at startup
    internal var _mapboxAccessToken: String = ""

    internal var _apiBaseUrl: String = "" // Set by BuildConfig via product flavor
    
    actual val MAPBOX_ACCESS_TOKEN: String
        get() = _mapboxAccessToken
    
    actual val API_BASE_URL: String
        get() = _apiBaseUrl
    
    /**
     * Initialize config values from the app module's BuildConfig
     * Call this from MainActivity or Application.onCreate()
     */
    fun initialize(mapboxAccessToken: String, apiBaseUrl: String) {
        _mapboxAccessToken = mapboxAccessToken
        _apiBaseUrl = apiBaseUrl
    }
}
