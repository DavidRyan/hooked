package com.hooked.catches.presentation.model

sealed class CatchGridEffect {
    data class NavigateToCatchDetails(val catchId: String) : CatchGridEffect()
    data class ShowError(val message: String) : CatchGridEffect()
    data class ShowSuccess(val message: String) : CatchGridEffect()
}
