package com.hooked.catches.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SubmitCatchDto(
    val species: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    @kotlinx.serialization.SerialName("caught_at") val caughtAt: String?,
    val notes: String? = null,
    val imageBytes: ByteArray? = null
)
