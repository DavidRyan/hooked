package com.hooked.catches.presentation

import com.hooked.catches.domain.entities.CatchEnrichmentUpdate
import com.hooked.catches.domain.entities.EnrichmentStatus
import com.hooked.catches.domain.usecases.GetCatchDetailsUseCase
import com.hooked.catches.domain.usecases.ObserveCatchEnrichmentUpdatesUseCase
import com.hooked.catches.presentation.model.CatchDetailsEffect
import com.hooked.catches.presentation.model.CatchDetailsIntent
import com.hooked.catches.presentation.model.CatchDetailsState
import com.hooked.catches.presentation.model.toCatchDetailsModel
import com.hooked.core.HookedViewModel
import com.hooked.core.domain.UseCaseResult
import com.hooked.core.logging.Logger
import kotlinx.coroutines.launch

class CatchDetailsViewModel(
    private val getCatchDetailsUseCase: GetCatchDetailsUseCase,
    private val observeCatchEnrichmentUpdates: ObserveCatchEnrichmentUpdatesUseCase
) : HookedViewModel<CatchDetailsIntent, CatchDetailsState, CatchDetailsEffect>() {

    companion object {
        private const val TAG = "CatchDetailsViewModel"
    }

    private var currentCatchId: String? = null

    init {
        observeEnrichmentUpdates()
    }

    override fun handleIntent(intent: CatchDetailsIntent) {
        when (intent) {
            is CatchDetailsIntent.LoadCatchDetails -> {
                currentCatchId = intent.catchId
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

    private fun observeEnrichmentUpdates() {
        viewModelScope.launch {
            observeCatchEnrichmentUpdates().collect { update ->
                val catchId = when (update) {
                    is CatchEnrichmentUpdate.Completed -> update.catchId
                    is CatchEnrichmentUpdate.Failed -> update.catchId
                }

                if (catchId != currentCatchId) return@collect

                when (update) {
                    is CatchEnrichmentUpdate.Completed -> {
                        // Reload to pick up newly populated fields (species, weight, etc.)
                        loadCatchDetails(catchId)
                    }
                    is CatchEnrichmentUpdate.Failed -> {
                        setState {
                            copy(
                                catchDetails = catchDetails?.copy(
                                    enrichmentStatus = EnrichmentStatus.Failed
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun createInitialState(): CatchDetailsState {
        return CatchDetailsState()
    }
}
