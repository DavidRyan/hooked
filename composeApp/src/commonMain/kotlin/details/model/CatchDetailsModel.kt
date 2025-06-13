package details.model

data class CatchDetailsModel(
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
}