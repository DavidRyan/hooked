package com.hooked.submit.domain.entities

data class SubmitCatchEntity(
    val species: String,
    val weight: Double,
    val length: Double,
    val latitude: Double?,
    val longitude: Double?,
    val photoBytes: ByteArray?,
    val timestamp: Long
)
