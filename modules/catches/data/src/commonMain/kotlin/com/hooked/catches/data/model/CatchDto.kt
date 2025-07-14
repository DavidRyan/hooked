package com.hooked.catches.data.model

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.entities.CatchDetailsEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatchDto(
    val id: String,
    val species: String,
    val location: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("caught_at") val caughtAt: String,
    val notes: String? = null,
    @SerialName("weather_data") val weatherData: Map<String, String>? = null,
    @SerialName("exif_data") val exifData: Map<String, String>? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("image_filename") val imageFilename: String? = null,
    @SerialName("image_content_type") val imageContentType: String? = null,
    @SerialName("image_file_size") val imageFileSize: Long? = null,
    @SerialName("inserted_at") val insertedAt: String,
    @SerialName("updated_at") val updatedAt: String
)

fun CatchDto.toEntity(): CatchEntity {
    return CatchEntity(
        id = id,
        name = species,
        description = notes ?: "Caught a $species at $location",
        dateCaught = caughtAt.take(10), // Extract date part from datetime string
        location = location,
        imageUrl = imageUrl ?: "",
        weight = 0.0, // Weight not in current schema, using default
        length = 0.0  // Length not in current schema, using default
    )
}

fun CatchDto.toCatchDetailsEntity(): CatchDetailsEntity {
    return CatchDetailsEntity(
        id = id,
        species = species,
        weight = 0.0, // Weight not in current schema, using default
        length = 0.0, // Length not in current schema, using default
        latitude = latitude,
        longitude = longitude,
        timestamp = null, // Convert datetime string to timestamp if needed
        photoUrl = imageUrl ?: "",
        location = location,
        dateCaught = caughtAt.take(10) // Extract date part from datetime string
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