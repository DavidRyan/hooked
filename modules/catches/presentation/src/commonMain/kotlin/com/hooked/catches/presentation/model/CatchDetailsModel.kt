package com.hooked.catches.presentation.model

import kotlinx.serialization.Serializable

@Serializable
data class CatchDetailsModel(
    val id: String,
    val species: String?,
    val weight: Double?,
    val length: Double?,
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long?,
    val photoUrl: String?,
    val location: String?,
    val weatherData: Map<String, String?>?
)
