package com.hooked.mcp.models

import kotlinx.serialization.Serializable

@Serializable
data class CatchSummary(
    val id: String,
    val species: String?,
    val location: String?,
    val caughtAt: String?,
    val hasNotes: Boolean,
    val imageUrl: String?
)
