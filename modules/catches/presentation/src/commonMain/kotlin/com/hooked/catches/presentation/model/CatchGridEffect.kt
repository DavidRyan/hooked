package com.hooked.catches.presentation.model

sealed class CatchGridEffect {
    data class NavigateToCatchDetails(val catchId: Long) : CatchGridEffect()
    data class ShowError(val message: String) : CatchGridEffect()
}
