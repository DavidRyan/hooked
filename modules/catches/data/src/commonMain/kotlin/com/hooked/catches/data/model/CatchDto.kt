package com.hooked.catches.data.model

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.entities.CatchDetailsEntity
import kotlinx.serialization.Serializable

@Serializable
data class CatchDto(
    val id: Long,
    val species: String,
    val weight: Double,
    val length: Double,
    val photoUrl: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long? = null
)

fun CatchDto.toEntity(): CatchEntity {
    return CatchEntity(
        id = id,
        name = species,
        description = "Caught a $species weighing $weight kg and measuring $length cm",
        dateCaught = "2023-10-01",
        location = "Unknown",
        imageUrl = photoUrl,
        weight = weight,
        length = length
    )
}

fun CatchDto.toCatchDetailsEntity(): CatchDetailsEntity {
    return CatchDetailsEntity(
        id = id,
        species = species,
        weight = weight,
        length = length,
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp,
        photoUrl = photoUrl,
        location = if (latitude != null && longitude != null) {
            "${latitude.toString().take(6)}, ${longitude.toString().take(6)}"
        } else "Unknown location",
        dateCaught = timestamp?.let { 
            "Date: $it" // TODO: Use kotlinx-datetime for proper date formatting
        } ?: "Unknown date"
    )
}

sealed class CatchResult {
    data class Success(val catches: List<CatchDto>) : CatchResult()
    data class Error(val message: String) : CatchResult()
    object Loading : CatchResult()
}

sealed class CatchDetailsResult {
    data class Success(val catch: CatchDto) : CatchDetailsResult()
    data class Error(val message: String) : CatchDetailsResult()
    object Loading : CatchDetailsResult()
}