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
