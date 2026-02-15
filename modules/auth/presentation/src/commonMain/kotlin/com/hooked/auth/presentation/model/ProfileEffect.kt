package com.hooked.auth.presentation.model

sealed class ProfileEffect {
    object NavigateToLogin : ProfileEffect()
    data class ShowError(val message: String) : ProfileEffect()
}
