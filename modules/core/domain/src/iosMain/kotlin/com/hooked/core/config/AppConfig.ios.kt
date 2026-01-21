package com.hooked.core.config

import platform.Foundation.NSBundle

/**
 * iOS implementation reads from Info.plist, which can be populated from xcconfig
 */
actual object AppConfig {
    actual val MAPBOX_ACCESS_TOKEN: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("MAPBOX_ACCESS_TOKEN") as? String ?: ""
    
    actual val API_BASE_URL: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("API_BASE_URL") as? String ?: "http://localhost:4000/api"
}
