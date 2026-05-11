package com.hooked.catches.domain.entities

data class CatchEntity(
    val id: String,
    val name: String?,
    val description: String?,
    val dateCaught: String?,
    val location: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val weight: Double? = null,
    val length: Double? = null,
    val enrichmentStatus: EnrichmentStatus = EnrichmentStatus.Pending
)
