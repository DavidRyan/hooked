package com.hooked.catches.data.mappers

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.entities.CatchDetailsEntity
import com.hooked.shared.network.model.CatchDto

/**
 * Maps DTO to domain entity
 */
fun CatchDto.toEntity(): CatchEntity {
    return CatchEntity(
        id = id,
        species = species,
        weight = weight,
        length = length,
        photoUrl = photoUrl,
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp
    )
}

/**
 * Maps DTO to detailed domain entity
 */
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
            // Convert timestamp to readable date - simplified for KMP compatibility
            "Date: $it" // TODO: Use kotlinx-datetime for proper date formatting
        } ?: "Unknown date"
    )
}