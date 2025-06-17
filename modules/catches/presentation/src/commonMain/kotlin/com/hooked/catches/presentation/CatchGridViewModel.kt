package com.hooked.catches.presentation

import com.hooked.core.HookedViewModel
import com.hooked.catches.domain.usecases.GetCatchesUseCase
import com.hooked.catches.domain.usecases.GetCatchesUseCaseResult
import kotlinx.coroutines.launch
import com.hooked.catches.presentation.model.CatchGridEffect
import com.hooked.catches.presentation.model.CatchGridIntent
import com.hooked.catches.presentation.model.CatchGridState
import com.hooked.catches.presentation.model.CatchModel
import com.hooked.catches.presentation.model.fromEntity

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