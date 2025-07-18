package com.hooked.auth.data.config

import com.hooked.core.config.NetworkConfig

object AuthConfig {
    val BASE_URL = NetworkConfig.BASE_URL
    const val LOGIN_ENDPOINT = "/auth/login"
    const val REGISTER_ENDPOINT = "/auth/register"
    const val ME_ENDPOINT = "/auth/me"
    const val REFRESH_ENDPOINT = "/auth/refresh"
}