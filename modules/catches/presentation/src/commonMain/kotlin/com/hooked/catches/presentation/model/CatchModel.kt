package com.hooked.catches.presentation.model

import com.hooked.catches.domain.entities.CatchEntity

data class CatchModel(
    val id: String,
    val name: String?,
    val imageUrl: String
)

fun fromEntity(entity: CatchEntity): CatchModel {
    return CatchModel(
        id = entity.id,
        name = entity.name,
        imageUrl = entity.imageUrl ?: ""
    )
}