package details.model

@Serializable
data class CatchDetailsModel(
    val id: Long,
    val species: String,
    val weight: Double,
    val length: Double,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val photoUrl: String
)