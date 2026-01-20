package com.hooked.catches.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubmitCatchDto(
    val species: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerialName("caught_at") val caughtAt: String?,
    val notes: String? = null,
    @SerialName("image_base64") val imageBase64: String? = null
)