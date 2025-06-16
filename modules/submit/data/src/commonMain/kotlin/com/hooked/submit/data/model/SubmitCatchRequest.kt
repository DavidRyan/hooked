package com.hooked.submit.data.model

data class SubmitCatchRequest(
    val species: String,
    val weight: Double,
    val length: Double,
    val latitude: Double?,
    val longitude: Double?,
    val photoBase64: String? = null,
    val timestamp: Long = 0L
)