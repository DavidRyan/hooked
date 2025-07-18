package com.hooked.auth.presentation.model

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isEmailError: Boolean = false,
    val isPasswordError: Boolean = false,
    val emailErrorMessage: String = "",
    val passwordErrorMessage: String = ""
)