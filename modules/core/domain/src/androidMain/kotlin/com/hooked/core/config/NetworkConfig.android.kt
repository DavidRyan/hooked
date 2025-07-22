package com.hooked.core.config

import com.hooked.core.logging.Logger

actual object NetworkConfig {
    actual val BASE_URL: String = "http://10.0.2.2:4000/api".also {
        Logger.debug("NetworkConfig", "Android BASE_URL = $it")
    }
}