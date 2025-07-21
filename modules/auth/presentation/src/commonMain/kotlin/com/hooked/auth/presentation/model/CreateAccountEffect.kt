package com.hooked.auth.presentation.model

sealed class CreateAccountEffect {
    object NavigateToHome : CreateAccountEffect()
    data class ShowError(val message: String) : CreateAccountEffect()
}