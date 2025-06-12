package grid.model

data class CatchGridState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val catches: List<CatchModel> = emptyList(),
) {
    companion object {
        val Empty = CatchGridState()
    }
}