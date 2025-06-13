package com.hooked.domain

import kotlinx.serialization.Serializable

@Serializable
data class CatchModel(
    val id: Long,
    val species: String,
    val weight: Double,
    val length: Double,
    val photoUrl: String
)
