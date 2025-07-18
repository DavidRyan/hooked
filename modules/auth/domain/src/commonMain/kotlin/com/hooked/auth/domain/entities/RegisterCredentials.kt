package com.hooked.auth.domain.entities

data class RegisterCredentials(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)