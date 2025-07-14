package com.hooked.catches.domain.entities

data class CatchDetailsEntity(
    val id: String,
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