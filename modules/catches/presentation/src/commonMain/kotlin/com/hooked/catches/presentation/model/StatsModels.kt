package com.hooked.catches.presentation.model

import com.hooked.catches.domain.entities.SpeciesData

data class StatsState(
    val isLoading: Boolean = false,
    val totalCatches: Int = 0,
    val speciesData: List<SpeciesData> = emptyList(),
    val uniqueSpecies: Int = 0,
    val uniqueLocations: Int = 0,
    val averageWeight: String? = null,
    val averageLength: String? = null,
    val biggestCatchName: String? = null,
    val biggestCatchWeight: String? = null,
    val aiInsights: String? = null,
    val isLoadingInsights: Boolean = false,
    val error: String? = null
)

sealed class StatsIntent {
    object LoadStats : StatsIntent()
    object LoadInsights : StatsIntent()
    object Refresh : StatsIntent()
    object NavigateBack : StatsIntent()
}

sealed class StatsEffect {
    object NavigateBack : StatsEffect()
    data class ShowError(val message: String) : StatsEffect()
}