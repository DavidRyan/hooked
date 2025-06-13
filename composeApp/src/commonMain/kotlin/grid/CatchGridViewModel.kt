package grid

import core.HookedViewModel
import com.hooked.domain.CatchGridEffect
import com.hooked.domain.CatchGridIntent
import com.hooked.domain.CatchGridState
import com.hooked.domain.CatchModel
import com.hooked.domain.usecase.GetCatchesUseCase

class CatchGridViewModel(
    private val getCatchesUseCase: GetCatchesUseCase
) : HookedViewModel<CatchGridIntent, CatchGridState, CatchGridEffect>() {

    override fun handleIntent(intent: CatchGridIntent) {
        when (intent) {
            is CatchGridIntent.LoadCatches -> {
                loadCatches()
            }

            is CatchGridIntent.NavigateCatchDetails -> sendEffect {
                CatchGridEffect.NavigateCatchDetails(intent.id)
            }
        }
    }

    private fun loadCatches() {
        viewModelScope.launch {
            try {
                val catches = getCatchesUseCase()
                setState { copy(catches = catches, isLoading = false) }
            } catch (e: Exception) {
                sendEffect { CatchGridEffect.OnError(e.message ?: "An unknown error occurred") }
            }
        }
    }

    override fun createInitialState(): CatchGridState {
        return CatchGridState()
    }
}