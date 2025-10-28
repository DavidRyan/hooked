package com.hooked.catches.domain.entities

data class StatsEntity(
    val totalCatches: Int,
    val speciesBreakdown: Map<String, Int>,
    val uniqueSpecies: Int,
    val uniqueLocations: Int,
    val averageWeight: Double?,
    val averageLength: Double?,
    val biggestCatch: CatchEntity?,
    val mostRecentCatch: CatchEntity?
)

data class SpeciesData(
    val name: String,
    val count: Int,
    val percentage: Float
)