package details

import core.HookedViewModel
import details.model.CatchDetailsEffect
import details.model.CatchDetailsIntent
import details.model.CatchDetailsState

class CatchDetailsViewModel : HookedViewModel<CatchDetailsIntent, CatchDetailsState, CatchDetailsEffect>() {

    override fun handleIntent(intent: CatchDetailsIntent) {
        when (intent) {
            else -> {}
        }
    }

    override fun createInitialState(): CatchDetailsState {
        return CatchDetailsState.Empty
    }
}