package com.hooked.mcp.models

import kotlinx.serialization.Serializable

@Serializable
data class SkunkSummary(
    val id: String,
    val location: String?,
    val fishedAt: String?,
    val notes: String?,
    val enrichmentStatus: Boolean
)
