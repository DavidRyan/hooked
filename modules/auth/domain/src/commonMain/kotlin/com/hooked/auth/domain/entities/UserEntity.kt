package com.hooked.auth.domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class UserEntity(
    val id: String,
    val email: String,
    val username: String,
    val token: String? = null
)