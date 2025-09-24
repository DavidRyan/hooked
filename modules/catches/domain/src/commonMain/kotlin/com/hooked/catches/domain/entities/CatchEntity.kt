package com.hooked.catches.domain.entities

data class CatchEntity(
    val id: String,
    val name: String?,
    val description: String?,
    val dateCaught: String?,
    val location: String?,
    val imageUrl: String? = null,
    val weight: Double? = null,
    val length: Double? = null
)