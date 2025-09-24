package com.hooked.catches.data.database

import com.hooked.catches.domain.entities.CatchEntity as DomainCatchEntity
import com.hooked.catches.domain.entities.CatchDetailsEntity

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
    return CatchDetailsEntity(
        id = id,
        species = species,
        weight = 0.0, // Weight not in current schema, using default
        length = 0.0, // Length not in current schema, using default
        latitude = latitude,
        longitude = longitude,
        timestamp = null, // Convert datetime string to timestamp if needed
        photoUrl = image_url ?: "",
        location = location,
        dateCaught = caught_at?.take(10) // Extract date part from datetime string
    )
}