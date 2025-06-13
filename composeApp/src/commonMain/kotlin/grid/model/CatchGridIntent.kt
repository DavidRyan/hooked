package grid.model

sealed class CatchGridIntent {
    data object LoadCatches : CatchGridIntent()
    data class NavigateToCatchDetails(val catchId: Long) : CatchGridIntent()
}
