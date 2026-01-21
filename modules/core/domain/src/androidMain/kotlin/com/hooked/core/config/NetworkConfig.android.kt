package com.hooked.core.config

import com.hooked.core.logging.Logger

actual object NetworkConfig {
    actual val BASE_URL: String
        get() = AppConfig.API_BASE_URL.also {
            Logger.debug("NetworkConfig", "Android BASE_URL = $it")
        }
}