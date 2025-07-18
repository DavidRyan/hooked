package com.hooked.auth.presentation.model

sealed class LoginEffect {
    object NavigateToHome : LoginEffect()
    data class ShowError(val message: String) : LoginEffect()
}