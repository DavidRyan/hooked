package com.hooked.shared.network.model

import kotlinx.serialization.Serializable

@Serializable
data class CatchDto(
    val id: Long,
    val species: String,
    val weight: Double,
    val length: Double,
    val photoUrl: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long? = null
)