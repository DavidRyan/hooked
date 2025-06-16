package com.hooked.features.catches.presentation.model

import domain.model.CatchEntity

data class CatchModel(
    val id: Long,
    val name: String,
    val imageUrl: String
)

fun fromEntity(entity: CatchEntity): CatchModel {
    return CatchModel(
        id = entity.id,
        name = entity.name,
        imageUrl = entity.imageUrl ?: ""
    )
}