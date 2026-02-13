package com.hooked.skunks.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SkunkDto(
    val id: String,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("fished_at") val fishedAt: String? = null,
    val notes: String? = null,
    @SerialName("weather_data") val weatherData: Map<String, String?>? = null,
    @SerialName("enrichment_status") val enrichmentStatus: Boolean = false,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("inserted_at") val insertedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
