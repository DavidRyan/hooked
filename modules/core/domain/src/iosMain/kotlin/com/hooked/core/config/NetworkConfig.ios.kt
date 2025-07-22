package com.hooked.core.config

import com.hooked.core.logging.Logger

actual object NetworkConfig {
    actual val BASE_URL: String = "http://localhost:4000/api".also {
        Logger.debug("NetworkConfig", "iOS BASE_URL = $it")
    }
}