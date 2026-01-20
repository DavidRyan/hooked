package com.hooked.catches.presentation

import com.hooked.core.HookedViewModel
import com.hooked.catches.domain.usecases.GetCatchStatsUseCase
import com.hooked.catches.domain.usecases.GetFishingInsightsUseCase
import com.hooked.catches.domain.entities.SpeciesData
import com.hooked.core.domain.UseCaseResult
import kotlinx.coroutines.launch
import com.hooked.catches.presentation.model.StatsEffect
import com.hooked.catches.presentation.model.StatsIntent
import com.hooked.catches.presentation.model.StatsState
import com.hooked.core.logging.Logger

class StatsViewModel(
    private val getCatchStatsUseCase: GetCatchStatsUseCase,
    private val getFishingInsightsUseCase: GetFishingInsightsUseCase
) : HookedViewModel<StatsIntent, StatsState, StatsEffect>() {

    companion object {
        private const val TAG = "StatsViewModel"
    }

    init {
        handleIntent(StatsIntent.LoadStats)
        handleIntent(StatsIntent.LoadInsights)
    }

    override fun handleIntent(intent: StatsIntent) {
        when (intent) {
            is StatsIntent.LoadStats -> loadStats()
            is StatsIntent.LoadInsights -> loadInsights()
            is StatsIntent.Refresh -> {
                loadStats()
                loadInsights()
            }
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
                                averageWeight = stats.averageWeight?.let { "${it.toString()} lbs" },
                                averageLength = stats.averageLength?.let { "${it.toString()} in" },
                                biggestCatchName = stats.biggestCatch?.name,
                                biggestCatchWeight = stats.biggestCatch?.weight?.let { "${it.toString()} lbs" }
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
                Logger.error(TAG, "Failed to load stats: ${e.message}", e)
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
    
    private fun loadInsights() {
        setState { copy(isLoadingInsights = true) }
        
        viewModelScope.launch {
            try {
                when (val result = getFishingInsightsUseCase()) {
                    is UseCaseResult.Success -> {
                        setState {
                            copy(
                                isLoadingInsights = false,
                                aiInsights = result.data.insights
                            )
                        }
                    }
                    is UseCaseResult.Error -> {
                        setState {
                            copy(
                                isLoadingInsights = false,
                                aiInsights = null
                            )
                        }
                        result.throwable?.let { throwable ->
                            Logger.error(TAG, "Failed to load fishing insights: ${result.message}", throwable)
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Failed to load fishing insights: ${e.message}", e)
                setState {
                    copy(
                        isLoadingInsights = false,
                        aiInsights = null
                    )
                }
            }
        }
    }

    override fun createInitialState(): StatsState {
        return StatsState()
    }
}
