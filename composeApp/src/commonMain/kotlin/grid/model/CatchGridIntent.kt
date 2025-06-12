package grid.model

sealed class CatchGridIntent {
    data class LoadCatches(val forceRefresh: Boolean = false) : CatchGridIntent()
    data class NavigateCatchDetails(val id: Long) : CatchGridIntent()
}
