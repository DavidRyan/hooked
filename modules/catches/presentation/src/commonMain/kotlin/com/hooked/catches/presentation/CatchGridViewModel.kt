package com.hooked.catches.presentation

import com.hooked.core.HookedViewModel
import com.hooked.catches.domain.usecases.GetCatchesUseCase
import com.hooked.core.domain.UseCaseResult
import kotlinx.coroutines.launch
import com.hooked.catches.presentation.model.CatchGridEffect
import com.hooked.catches.presentation.model.CatchGridIntent
import com.hooked.catches.presentation.model.CatchGridState
import com.hooked.catches.presentation.model.CatchModel
import com.hooked.catches.presentation.model.fromEntity
import com.hooked.core.logging.logError

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
            try {
                when (val result = getCatchesUseCase()) {
                    is UseCaseResult.Success -> {
                        setState { copy(catches = result.data.map { fromEntity(it) }, isLoading = false) }
                    }

                    is UseCaseResult.Error -> {
                        setState { copy(isLoading = false) }
                        sendEffect { CatchGridEffect.ShowError(result.message) }
                    }
                }
            } catch (e: Exception) {
                logError("Failed to load catches", e)
                setState { copy(isLoading = false) }
                sendEffect { CatchGridEffect.ShowError("Failed to load catches: ${e.message}") }
            }
        }
    }

    override fun createInitialState(): CatchGridState {
        return CatchGridState()
    }
}