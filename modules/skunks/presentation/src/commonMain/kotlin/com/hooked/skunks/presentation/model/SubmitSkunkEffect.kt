package com.hooked.skunks.presentation.model

sealed class SubmitSkunkEffect {
    data object SubmitSuccess : SubmitSkunkEffect()
    data class SubmitError(val message: String) : SubmitSkunkEffect()
    data object RequestLocationPermission : SubmitSkunkEffect()
}
