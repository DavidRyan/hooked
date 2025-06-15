package data.model

import kotlinx.serialization.Serializable

@Serializable
data class CatchDto(
    val id: Long,
    val species: String,
    val weight: Double,
    val length: Double,
    val photoUrl: String
)

sealed class CatchResult {
    data class Success(val catches: List<CatchDto>) : CatchResult()
    data class Error(val message: String) : CatchResult()
    object Loading : CatchResult()
}