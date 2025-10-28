package com.hooked.catches.presentation

import com.hooked.core.HookedViewModel
import com.hooked.catches.domain.usecases.GetCatchStatsUseCase
import com.hooked.catches.domain.entities.SpeciesData
import com.hooked.core.domain.UseCaseResult
import kotlinx.coroutines.launch
import com.hooked.catches.presentation.model.StatsEffect
import com.hooked.catches.presentation.model.StatsIntent
import com.hooked.catches.presentation.model.StatsState
import com.hooked.core.logging.logError

class StatsViewModel(
    private val getCatchStatsUseCase: GetCatchStatsUseCase
) : HookedViewModel<StatsIntent, StatsState, StatsEffect>() {

    init {
        handleIntent(StatsIntent.LoadStats)
    }

    override fun handleIntent(intent: StatsIntent) {
        when (intent) {
            is StatsIntent.LoadStats -> loadStats()
            is StatsIntent.Refresh -> loadStats()
            is StatsIntent.NavigateBack -> sendEffect { StatsEffect.NavigateBack }
        }
    }

    private fun loadStats() {
        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                when (val result = getCatchStatsUseCase()) {
                    is UseCaseResult.Success -> {
                        val stats = result.data

                        val totalCount = stats.totalCatches
                        val speciesData = stats.speciesBreakdown.map { (name, count) ->
                            SpeciesData(
                                name = name,
                                count = count,
                                percentage = if (totalCount > 0) count.toFloat() / totalCount else 0f
                            )
                        }.sortedByDescending { it.count }

                        setState {
                            copy(
                                isLoading = false,
                                totalCatches = stats.totalCatches,
                                speciesData = speciesData,
                                uniqueSpecies = stats.uniqueSpecies,
                                uniqueLocations = stats.uniqueLocations,
                                averageWeight = stats.averageWeight?.let { "%.1f lbs".format(it) },
                                averageLength = stats.averageLength?.let { "%.1f in".format(it) },
                                biggestCatchName = stats.biggestCatch?.name,
                                biggestCatchWeight = stats.biggestCatch?.weight?.let { "%.1f lbs".format(it) }
                            )
                        }
                    }
                    is UseCaseResult.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                        sendEffect { StatsEffect.ShowError(result.message) }
                    }
                }
            } catch (e: Exception) {
                logError("Failed to load stats", e)
                setState {
                    copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                sendEffect { StatsEffect.ShowError("Failed to load stats: ${e.message}") }
            }
        }
    }

    override fun createInitialState(): StatsState {
        return StatsState()
    }
}