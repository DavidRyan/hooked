package com.hooked.auth.presentation.model

sealed class LoginIntent {
    data class UpdateEmail(val email: String) : LoginIntent()
    data class UpdatePassword(val password: String) : LoginIntent()
    object Login : LoginIntent()
    object ClearErrors : LoginIntent()
}