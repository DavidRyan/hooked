package com.hooked.catches.presentation.model

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.entities.EnrichmentStatus

data class CatchModel(
    val id: String,
    val name: String?,
    val imageUrl: String,
    val enrichmentStatus: EnrichmentStatus,
    val dateCaught: String?,
    val location: String?
)

fun fromEntity(entity: CatchEntity): CatchModel {
    return CatchModel(
        id = entity.id,
        name = entity.name,
        imageUrl = entity.imageUrl ?: "",
        enrichmentStatus = entity.enrichmentStatus,
        dateCaught = entity.dateCaught,
        location = entity.location
    )
}
