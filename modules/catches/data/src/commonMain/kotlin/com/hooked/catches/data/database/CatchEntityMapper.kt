package com.hooked.catches.data.database

import com.hooked.catches.domain.entities.CatchEntity as DomainCatchEntity
import com.hooked.catches.domain.entities.CatchDetailsEntity

fun CatchEntity.toDomainEntity(): DomainCatchEntity {
    return DomainCatchEntity(
        id = id,
        name = species,
        description = description,
        dateCaught = dateCaught,
        location = location,
        imageUrl = photoUrl,
        weight = weight,
        length = length
    )
}

fun CatchEntity.toCatchDetailsEntity(): CatchDetailsEntity {
    return CatchDetailsEntity(
        id = id,
        species = species,
        weight = weight,
        length = length,
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp,
        photoUrl = photoUrl,
        location = location,
        dateCaught = dateCaught
    )
}