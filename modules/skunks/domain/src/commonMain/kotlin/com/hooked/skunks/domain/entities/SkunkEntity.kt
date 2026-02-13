package com.hooked.skunks.domain.entities

data class SkunkEntity(
    val id: String,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val fishedAt: String?,
    val notes: String?,
    val weatherData: Map<String, Any?>?,
    val enrichmentStatus: Boolean,
    val createdAt: String?,
    val updatedAt: String?
)

data class SubmitSkunkEntity(
    val latitude: Double?,
    val longitude: Double?,
    val fishedAt: String,
    val notes: String?
)
