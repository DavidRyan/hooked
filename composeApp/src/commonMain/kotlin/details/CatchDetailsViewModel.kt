package details

import core.HookedViewModel
import com.hooked.domain.CatchDetailsEffect
import com.hooked.domain.CatchDetailsIntent
import com.hooked.domain.CatchDetailsState
import com.hooked.domain.usecase.GetCatchDetailsUseCase

class CatchDetailsViewModel(
    private val getCatchDetailsUseCase: GetCatchDetailsUseCase
) : HookedViewModel<CatchDetailsIntent, CatchDetailsState, CatchDetailsEffect>() {

    override fun handleIntent(intent: CatchDetailsIntent) {
        when (intent) {
            is CatchDetailsIntent.LoadCatchDetails -> {
                loadCatchDetails(intent.catchId)
            }
        }
    }

    private fun loadCatchDetails(catchId: Long) {
        viewModelScope.launch {
            try {
                val catchDetails = getCatchDetailsUseCase(catchId)
                setState { copy(catchDetails = catchDetails, isLoading = false) }
            } catch (e: Exception) {
                sendEffect { CatchDetailsEffect.OnError(e.message ?: "An unknown error occurred") }
            }
        }
    }

    override fun createInitialState(): CatchDetailsState {
        return CatchDetailsState()
    }
}