package details

import core.HookedViewModel
import details.model.CatchDetailsEffect
import details.model.CatchDetailsIntent
import details.model.CatchDetailsState
import details.model.toCatchDetailsModel
import domain.usecase.GetCatchDetailsUseCase
import domain.usecase.GetCatchDetailsUseCaseResult
import kotlinx.coroutines.launch

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
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = getCatchDetailsUseCase(catchId)) {
                is GetCatchDetailsUseCaseResult.Success -> {
                    setState { 
                        copy(
                            catchDetails = result.catchDetails.toCatchDetailsModel(),
                            isLoading = false
                        )
                    }
                }
                is GetCatchDetailsUseCaseResult.Error -> {
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