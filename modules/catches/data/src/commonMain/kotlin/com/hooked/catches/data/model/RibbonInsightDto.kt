package com.hooked.catches.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RibbonInsightDto(
    val headline: String,
    val body: String
)
