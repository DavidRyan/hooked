package com.hooked.catches.domain.entities

data class SubmitCatchEntity(
    val species: String,
    val weight: Double,
    val length: Double,
    val latitude: Double?,
    val longitude: Double?,
    val photoBase64: String?,
    val timestamp: Long
)