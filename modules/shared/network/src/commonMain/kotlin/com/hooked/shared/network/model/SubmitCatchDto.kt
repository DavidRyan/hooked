package com.hooked.shared.network.model

import kotlinx.serialization.Serializable

@Serializable
data class SubmitCatchDto(
    val species: String,
    val weight: Double,
    val length: Double,
    val latitude: Double?,
    val longitude: Double?,
    val photoBase64: String?,
    val timestamp: Long
)