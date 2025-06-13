package details

import core.HookedViewModel
import com.hooked.domain.model.CatchDetailsEffect
import com.hooked.domain.model.CatchDetailsIntent
import com.hooked.domain.model.CatchDetailsState
import com.hooked.data.repository.CatchDetailsRepository

class CatchDetailsViewModel(
    private val catchDetailsRepository: CatchDetailsRepository
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
                val catchDetails = catchDetailsRepository.getCatchDetails(catchId)
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