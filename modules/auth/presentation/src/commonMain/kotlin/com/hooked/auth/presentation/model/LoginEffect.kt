package com.hooked.auth.presentation.model

sealed class LoginEffect {
    object NavigateToHome : LoginEffect()
    object NavigateToOnboarding : LoginEffect()
    data class ShowError(val message: String) : LoginEffect()
}