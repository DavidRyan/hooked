package com.hooked.features.catches.presentation

import com.hooked.core.HookedViewModel
import domain.usecase.GetCatchesUseCase
import domain.usecase.GetCatchesUseCaseResult
import kotlinx.coroutines.launch
import com.hooked.features.catches.presentation.model.CatchGridEffect
import com.hooked.features.catches.presentation.model.CatchGridIntent
import com.hooked.features.catches.presentation.model.CatchGridState
import com.hooked.features.catches.presentation.model.CatchModel
import com.hooked.features.catches.presentation.model.fromEntity

class CatchGridViewModel(
    private val getCatchesUseCase: GetCatchesUseCase
) : HookedViewModel<CatchGridIntent, CatchGridState, CatchGridEffect>() {

    override fun handleIntent(intent: CatchGridIntent) {
        when (intent) {
            is CatchGridIntent.LoadCatches -> {
                loadCatches()
                setState { copy(isLoading = true) }
            }

            is CatchGridIntent.NavigateToCatchDetails -> sendEffect {
                CatchGridEffect.NavigateToCatchDetails(intent.catchId)
            }
        }
    }

    private fun loadCatches() {
        viewModelScope.launch {
            when (val result = getCatchesUseCase()) {
                is GetCatchesUseCaseResult.Success -> {
                    setState { copy(catches = result.catches.map { fromEntity(it) }, isLoading = false) }
                }

                is GetCatchesUseCaseResult.Error -> {
                    setState { copy(isLoading = false) }
                    sendEffect { CatchGridEffect.ShowError(result.message) }
                }
            }
        }
    }

    override fun createInitialState(): CatchGridState {
        return CatchGridState()
    }
}