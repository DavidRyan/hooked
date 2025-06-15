package grid

import core.HookedViewModel
import domain.usecase.GetCatchesUseCase
import domain.usecase.GetCatchesUseCaseResult
import kotlinx.coroutines.launch
import grid.model.CatchGridEffect
import grid.model.CatchGridIntent
import grid.model.CatchGridState
import grid.model.CatchModel
import grid.model.fromEntity

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
            when (val catches = getCatchesUseCase()) {
                is GetCatchesUseCaseResult.Success -> {
                    setState { copy(catches = catches.catches.map { fromEntity(it) }, isLoading = false) }
                }

                is GetCatchesUseCaseResult.Error -> {
                    sendEffect { CatchGridEffect.ShowError(catches.message) }
                }
            }}
        setState { copy(catches = catches, isLoading = false) }
    }

    override fun createInitialState(): CatchGridState {
        return CatchGridState()
    }
}