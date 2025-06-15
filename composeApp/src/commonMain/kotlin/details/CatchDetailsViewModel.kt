package details

import core.HookedViewModel
import details.model.CatchDetailsEffect
import details.model.CatchDetailsIntent
import details.model.CatchDetailsState
import kotlinx.coroutines.launch

class CatchDetailsViewModel(
) : HookedViewModel<CatchDetailsIntent, CatchDetailsState, CatchDetailsEffect>() {

    override fun handleIntent(intent: CatchDetailsIntent) {
        when (intent) {
            is CatchDetailsIntent.LoadCatchDetails -> {
                loadCatchDetails(intent.catchId)
            }
        }
    }

    private fun loadCatchDetails(catchId: Long) {
/*
        viewModelScope.launch {
            try {
                val catchDetails = getCatchDetailsUseCase(catchId)
                setState { copy(catchDetails = catchDetails, isLoading = false) }
            } catch (e: Exception) {
                sendEffect { CatchDetailsEffect.OnError(e.message ?: "An unknown error occurred") }
            }
        }
*/
    }

    override fun createInitialState(): CatchDetailsState {
        return CatchDetailsState()
    }
}