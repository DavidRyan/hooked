package com.hooked.skunks.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubmitSkunkRequest(
    @SerialName("user_skunk") val userSkunk: SubmitSkunkBody
)

@Serializable
data class SubmitSkunkBody(
    @SerialName("fished_at") val fishedAt: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null
)
