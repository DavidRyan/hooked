package com.hooked.catches.presentation

import com.hooked.core.HookedViewModel
import com.hooked.catches.presentation.model.CatchDetailsEffect
import com.hooked.catches.presentation.model.CatchDetailsIntent
import com.hooked.catches.presentation.model.CatchDetailsState
import com.hooked.catches.presentation.model.toCatchDetailsModel
import com.hooked.catches.domain.usecases.GetCatchDetailsUseCase
import com.hooked.core.domain.UseCaseResult
import com.hooked.core.logging.Logger
import kotlinx.coroutines.launch

class CatchDetailsViewModel(
    private val getCatchDetailsUseCase: GetCatchDetailsUseCase
) : HookedViewModel<CatchDetailsIntent, CatchDetailsState, CatchDetailsEffect>() {

    companion object {
        private const val TAG = "CatchDetailsViewModel"
    }

    override fun handleIntent(intent: CatchDetailsIntent) {
        when (intent) {
            is CatchDetailsIntent.LoadCatchDetails -> {
                loadCatchDetails(intent.catchId)
            }
        }
    }

    private fun loadCatchDetails(catchId: String) {
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = getCatchDetailsUseCase(catchId)) {
                is UseCaseResult.Success -> {
                    setState { 
                        copy(
                            catchDetails = result.data.toCatchDetailsModel(),
                            isLoading = false
                        )
                    }
                }
                is UseCaseResult.Error -> {
                    Logger.error(TAG, "Failed to load catch details: ${result.message}")
                    setState { copy(isLoading = false) }
                    sendEffect { CatchDetailsEffect.OnError(result.message) }
                }
            }
        }
    }

    override fun createInitialState(): CatchDetailsState {
        return CatchDetailsState()
    }
}