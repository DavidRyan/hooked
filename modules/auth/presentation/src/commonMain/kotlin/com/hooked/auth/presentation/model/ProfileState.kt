package com.hooked.auth.presentation.model

data class ProfileState(
    val username: String = "",
    val email: String = "",
    val initials: String = "",
    val isLoading: Boolean = true,
    val isLoggingOut: Boolean = false
)
