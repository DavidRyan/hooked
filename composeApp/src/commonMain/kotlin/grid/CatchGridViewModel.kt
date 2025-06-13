package grid

import core.HookedViewModel
import com.hooked.domain.model.CatchGridEffect
import com.hooked.domain.model.CatchGridIntent
import com.hooked.domain.model.CatchGridState
import com.hooked.domain.model.CatchModel
import com.hooked.data.repository.CatchGridRepository

class CatchGridViewModel(
    private val catchGridRepository: CatchGridRepository
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
                val catches = catchGridRepository.getCatches()
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