package com.hooked.catches.presentation.model

sealed class CatchGridIntent {
    data object LoadCatches : CatchGridIntent()
    data class NavigateToCatchDetails(val catchId: String) : CatchGridIntent()
    data class DeleteCatch(val catchId: String) : CatchGridIntent()
    data class ShowDeleteDialog(val catchId: String) : CatchGridIntent()
    data object HideDeleteDialog : CatchGridIntent()
}
