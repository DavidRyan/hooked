package com.hooked.catches.presentation.model

import com.hooked.catches.domain.entities.CatchDetailsEntity


fun CatchDetailsEntity.toCatchDetailsModel(): CatchDetailsModel {
    return CatchDetailsModel(
        id = id,
        species = species,
        weight = weight,
        length = length,
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        timestamp = timestamp ?: 0L,
        photoUrl = photoUrl,
        location = location,
        weatherData = weatherData
    )
}