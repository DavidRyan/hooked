package grid.model

sealed class CatchGridIntent {
    data class LoadCatches(val forceRefresh: Boolean = false) : CatchGridIntent()
}
