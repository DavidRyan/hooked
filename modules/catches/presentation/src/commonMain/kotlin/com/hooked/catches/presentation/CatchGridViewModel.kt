package com.hooked.catches.presentation

import com.hooked.core.HookedViewModel
import com.hooked.catches.domain.usecases.GetCatchesUseCase
import com.hooked.catches.domain.usecases.DeleteCatchUseCase
import com.hooked.core.domain.UseCaseResult
import kotlinx.coroutines.launch
import com.hooked.catches.presentation.model.CatchGridEffect
import com.hooked.catches.presentation.model.CatchGridIntent
import com.hooked.catches.presentation.model.CatchGridState
import com.hooked.catches.presentation.model.CatchModel
import com.hooked.catches.presentation.model.fromEntity
import com.hooked.core.logging.Logger

class CatchGridViewModel(
    private val getCatchesUseCase: GetCatchesUseCase,
    private val deleteCatchUseCase: DeleteCatchUseCase
) : HookedViewModel<CatchGridIntent, CatchGridState, CatchGridEffect>() {

    companion object {
        private const val TAG = "CatchGridViewModel"
    }

    override fun handleIntent(intent: CatchGridIntent) {
        when (intent) {
            is CatchGridIntent.LoadCatches -> {
                val isRefresh = state.value.catches.isNotEmpty()
                
                if (isRefresh) {
                    setState { copy(isRefreshing = true) }
                } else {
                    setState { copy(isLoading = true) }
                }
                
                loadCatches()
            }

            is CatchGridIntent.NavigateToCatchDetails -> sendEffect {
                CatchGridEffect.NavigateToCatchDetails(intent.catchId)
            }
            
            is CatchGridIntent.ShowDeleteDialog -> {
                setState { 
                    copy(
                        showDeleteDialog = true,
                        catchToDelete = intent.catchId
                    )
                }
            }
            
            is CatchGridIntent.HideDeleteDialog -> {
                setState { 
                    copy(
                        showDeleteDialog = false,
                        catchToDelete = null
                    )
                }
            }
            
            is CatchGridIntent.DeleteCatch -> {
                deleteCatch(intent.catchId)
            }
        }
    }

    private fun loadCatches() {
        viewModelScope.launch {
            try {
                when (val result = getCatchesUseCase()) {
                    is UseCaseResult.Success -> {
                        setState { 
                            copy(
                                catches = result.data.map { fromEntity(it) }, 
                                isLoading = false,
                                isRefreshing = false
                            ) 
                        }
                    }

                    is UseCaseResult.Error -> {
                        setState { 
                            copy(
                                isLoading = false,
                                isRefreshing = false
                            ) 
                        }
                        sendEffect { CatchGridEffect.ShowError(result.message) }
                    }
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Failed to load catches: ${e.message}", e)
                setState { 
                    copy(
                        isLoading = false,
                        isRefreshing = false
                    ) 
                }
                sendEffect { CatchGridEffect.ShowError("Failed to load catches: ${e.message}") }
            }
        }
    }
    
    private fun deleteCatch(catchId: String) {
        viewModelScope.launch {
            try {
                setState { copy(showDeleteDialog = false, catchToDelete = null) }
                
                when (val result = deleteCatchUseCase(catchId)) {
                    is UseCaseResult.Success -> {
                        setState { 
                            copy(catches = catches.filter { it.id != catchId })
                        }
                        sendEffect { CatchGridEffect.ShowSuccess("Catch deleted successfully") }
                    }
                    is UseCaseResult.Error -> {
                        sendEffect { CatchGridEffect.ShowError(result.message) }
                    }
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Failed to delete catch: ${e.message}", e)
                sendEffect { CatchGridEffect.ShowError("Failed to delete catch: ${e.message}") }
            }
        }
    }

    override fun createInitialState(): CatchGridState {
        return CatchGridState()
    }
}
