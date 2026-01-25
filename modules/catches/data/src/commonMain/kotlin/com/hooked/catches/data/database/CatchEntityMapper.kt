package com.hooked.catches.data.database

import com.hooked.catches.domain.entities.CatchEntity as DomainCatchEntity
import com.hooked.catches.domain.entities.CatchDetailsEntity
import com.hooked.core.logging.Logger
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json

fun CatchEntity.toDomainEntity(): DomainCatchEntity {
    return DomainCatchEntity(
        id = id,
        name = species,
        description = notes ?: "Caught a $species at $location",
        dateCaught = caught_at?.take(10), // Extract date part from datetime string
        location = location,
        imageUrl = image_url,
        weight = 0.0, // Weight not in current schema, using default
        length = 0.0  // Length not in current schema, using default
    )
}

fun CatchEntity.toCatchDetailsEntity(): CatchDetailsEntity {
    val weatherData = weather_data?.let { jsonString ->
        try {
            Json.decodeFromString<Map<String, String?>>(jsonString)
                .takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Logger.error("CatchEntityMapper", "Failed to parse weather_data JSON for catch $id: ${e.message}", e)
            Logger.debug("CatchEntityMapper", "Invalid weather_data JSON: $jsonString")
            null
        }
    }

    return CatchDetailsEntity(
        id = id,
        species = species,
        weight = 0.0, // Weight not in current schema, using default
        length = 0.0, // Length not in current schema, using default
        latitude = latitude,
        longitude = longitude,
        timestamp = parseCaughtAtToTimestamp(caught_at),
        photoUrl = image_url ?: "",
        location = location,
        dateCaught = caught_at?.take(10), // Extract date part from datetime string
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
