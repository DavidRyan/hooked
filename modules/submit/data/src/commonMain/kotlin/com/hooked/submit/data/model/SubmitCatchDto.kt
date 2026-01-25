package com.hooked.submit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SubmitCatchDto(
    val species: String,
    val weight: Double,
    val length: Double,
    val latitude: Double?,
    val longitude: Double?,
    val photoBytes: ByteArray?,
    val timestamp: Long
)
