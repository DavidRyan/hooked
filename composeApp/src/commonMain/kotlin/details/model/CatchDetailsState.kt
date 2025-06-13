package details.model

data class CatchDetailsState(
    val catchId: String,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val catchDetails: CatchDetails? = null
) {
    data class CatchDetails(
        val id: String,
        val name: String,
        val description: String,
        val imageUrl: String
    )

    companion object {
        val Empty = CatchDetailsState(
            catchId = "",
            isLoading = true,
            errorMessage = null,
            catchDetails = null
        )
    }

}