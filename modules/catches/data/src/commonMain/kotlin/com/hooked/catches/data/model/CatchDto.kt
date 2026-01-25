package com.hooked.catches.data.model

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.entities.CatchDetailsEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant

@Serializable
data class CatchDto(
    val id: String,
    val species: String?,
    val location: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("caught_at") val caughtAt: String?,
    val notes: String? = null,
    @SerialName("weather_data") val weatherData: Map<String, String?>? = null,
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
        dateCaught = caughtAt?.take(10), // Extract date part from datetime string
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
        timestamp = parseCaughtAtToTimestamp(caughtAt),
        photoUrl = imageUrl ?: "",
        location = location,
        dateCaught = caughtAt?.take(10), // Extract date part from datetime string
        weatherData = weatherData
    )
}

private fun parseCaughtAtToTimestamp(caughtAt: String?): Long? {
    if (caughtAt.isNullOrBlank()) {
        return null
    }

    return runCatching {
        Instant.parse(caughtAt).toEpochMilliseconds()
    }.getOrElse { _: Throwable ->
        runCatching {
            LocalDateTime.parse(caughtAt)
                .toInstant(TimeZone.UTC)
                .toEpochMilliseconds()
        }.getOrNull() ?: runCatching {
            LocalDate.parse(caughtAt)
                .atStartOfDayIn(TimeZone.UTC)
                .toEpochMilliseconds()
        }.getOrNull()
    }
}
