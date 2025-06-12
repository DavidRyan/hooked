package grid.model

sealed class CatchGridEffect {
    data class NavigateCatchDetails(val id: Long) : CatchGridEffect()
}