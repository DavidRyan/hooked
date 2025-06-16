package domain.model

import data.model.CatchDto

data class CatchDetailsEntity(
    val id: Long,
    val species: String,
    val weight: Double,
    val length: Double,
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long?,
    val photoUrl: String,
    val location: String = "Unknown location",
    val dateCaught: String = "Unknown date"
)

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
            "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
        } else "Unknown location",
        dateCaught = timestamp?.let { 
            // Convert timestamp to readable date - simplified for KMP compatibility
            "Date: $it" // TODO: Use kotlinx-datetime for proper date formatting
        } ?: "Unknown date"
    )
}