package com.hooked.mcp.models

import kotlinx.serialization.Serializable

@Serializable
data class CatchStats(
    val totalCatches: Int,
    val speciesBreakdown: Map<String, Int>,
    val uniqueSpecies: Int,
    val uniqueLocations: Int,
    val mostProductiveLocation: String?,
    val mostProductiveLocationCount: Int,
    val dateRange: DateRange?
)

@Serializable
data class DateRange(
    val earliest: String,
    val latest: String
)
