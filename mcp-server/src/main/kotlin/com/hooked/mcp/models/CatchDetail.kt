package com.hooked.mcp.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CatchDetail(
    val id: String,
    val species: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val caughtAt: String?,
    val notes: String?,
    val weatherData: JsonElement?,
    val imageUrl: String?,
    val enrichmentStatus: Boolean?,
    val insertedAt: String,
    val updatedAt: String
)
