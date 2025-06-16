package com.hooked.catches.domain.entities

/**
 * Core catch entity for the domain layer
 */
data class CatchEntity(
    val id: Long,
    val species: String,
    val weight: Double,
    val length: Double,
    val photoUrl: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long? = null
)

/**
 * Detailed catch entity with additional information
 */
data class CatchDetailsEntity(
    val id: Long,
    val species: String,
    val weight: Double,
    val length: Double,
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long?,
    val photoUrl: String,
    val location: String = "Unknown location",
    val dateCaught: String = "Unknown date"
)