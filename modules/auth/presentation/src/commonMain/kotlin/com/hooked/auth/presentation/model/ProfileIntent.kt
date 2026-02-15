package com.hooked.auth.presentation.model

sealed class ProfileIntent {
    object LoadProfile : ProfileIntent()
    object Logout : ProfileIntent()
}
