package grid

import core.HookedViewModel
import grid.model.CatchGridEffect
import grid.model.CatchGridIntent
import grid.model.CatchGridState
import grid.model.CatchModel

class CatchGridViewModel : HookedViewModel<CatchGridIntent, CatchGridState, CatchGridEffect>() {

    override fun handleIntent(intent: CatchGridIntent) {
        when (intent) {
            is CatchGridIntent.LoadCatches -> setState {
                copy(isLoading = false, catches = listOf(
                    CatchModel(1, "Catch 1", ""),
                    CatchModel(2, "Catch 1", ""),
                    CatchModel(3, "Catch 1", ""),
                    CatchModel(5, "Catch 1", ""),
                ))
            }
        }
    }

    override fun createInitialState(): CatchGridState {
        return CatchGridState.Empty
    }
}