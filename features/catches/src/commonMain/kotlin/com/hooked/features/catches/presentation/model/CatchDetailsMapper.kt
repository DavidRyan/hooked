package com.hooked.features.catches.presentation.model

import domain.model.CatchDetailsEntity

fun CatchDetailsEntity.toCatchDetailsModel(): CatchDetailsModel {
    return CatchDetailsModel(
        id = id,
        species = species,
        weight = weight,
        length = length,
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        timestamp = timestamp ?: 0L,
        photoUrl = photoUrl
    )
}