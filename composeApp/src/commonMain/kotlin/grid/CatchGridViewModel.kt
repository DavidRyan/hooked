package grid

import core.HookedViewModel
import kotlinx.coroutines.launch
import grid.model.CatchGridEffect
import grid.model.CatchGridIntent
import grid.model.CatchGridState
import grid.model.CatchModel
import usecase.GetCatchesUseCase

class CatchGridViewModel(
    private val getCatchesUseCase: GetCatchesUseCase
) : HookedViewModel<CatchGridIntent, CatchGridState, CatchGridEffect>() {

    override fun handleIntent(intent: CatchGridIntent) {
        when (intent) {
            is CatchGridIntent.LoadCatches -> {
                loadCatches()
            }

            is CatchGridIntent.NavigateToCatchDetails -> sendEffect {
                CatchGridEffect.NavigateToCatchDetails(intent.catchId)
            }

            is CatchGridIntent.NavigateToCatchDetails -> TODO()
        }
    }

    private fun loadCatches() {
        viewModelScope.launch {
            try {
                val catches = getCatchesUseCase()
                setState { copy(catches = catches, isLoading = false) }
            } catch (e: Exception) {
                sendEffect { CatchGridEffect.ShowError(e.message ?: "An unknown error occurred") }
            }
        }
    }

    override fun createInitialState(): CatchGridState {
        return CatchGridState()
    }
}