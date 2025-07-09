package com.hooked.submit.presentation

import com.hooked.core.HookedViewModel
import com.hooked.submit.domain.entities.SubmitCatchEntity
import com.hooked.submit.domain.usecases.SubmitCatchUseCase
import com.hooked.submit.domain.usecases.ConvertImageToBase64UseCase
import com.hooked.core.domain.UseCaseResult
import com.hooked.core.domain.flatMap
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import com.hooked.submit.presentation.model.SubmitCatchEffect
import com.hooked.submit.presentation.model.SubmitCatchIntent
import com.hooked.submit.presentation.model.SubmitCatchState
import com.hooked.core.logging.logError

class SubmitCatchViewModel(
    private val submitCatchUseCase: SubmitCatchUseCase,
    private val convertImageToBase64UseCase: ConvertImageToBase64UseCase
) : HookedViewModel<SubmitCatchIntent, SubmitCatchState, SubmitCatchEffect>() {

    override fun handleIntent(intent: SubmitCatchIntent) {
        when (intent) {
            is SubmitCatchIntent.UpdateSpecies -> {
                setState { copy(species = intent.species) }
            }
            is SubmitCatchIntent.UpdateWeight -> {
                setState { copy(weight = intent.weight) }
            }
            is SubmitCatchIntent.UpdateLength -> {
                setState { copy(length = intent.length) }
            }
            is SubmitCatchIntent.UpdateLocation -> {
                setState { 
                    copy(
                        latitude = intent.latitude,
                        longitude = intent.longitude,
                        isLocationLoading = false
                    )
                }
            }
            is SubmitCatchIntent.UpdatePhoto -> {
                setState { copy(photoUri = intent.photoUri) }
            }
            is SubmitCatchIntent.GetCurrentLocation -> {
                setState { copy(isLocationLoading = true) }
                sendEffect { SubmitCatchEffect.RequestLocationPermission }
            }
            is SubmitCatchIntent.SubmitCatch -> {
                submitCatch()
            }
            is SubmitCatchIntent.NavigateBack -> {
                sendEffect { SubmitCatchEffect.NavigateBack }
            }
        }
    }

    private fun submitCatch() {
        val currentState = state.value
        
        if (!currentState.isFormValid) {
            sendEffect { SubmitCatchEffect.ShowError("Please fill in all required fields") }
            return
        }

        setState { copy(isSubmitting = true) }
        
        viewModelScope.launch {
            try {
                // Flat-mapped chain: Convert image -> Create entity -> Submit catch
                val result = if (currentState.photoUri != null) {
                    convertImageToBase64UseCase(currentState.photoUri!!)
                } else {
                    UseCaseResult.Success(null)
                }.flatMap { photoBase64 ->
                    val catchEntity = SubmitCatchEntity(
                        species = currentState.species,
                        weight = currentState.weight.toDouble(),
                        length = currentState.length.toDouble(),
                        latitude = currentState.latitude,
                        longitude = currentState.longitude,
                        photoBase64 = photoBase64,
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    )
                    submitCatchUseCase(catchEntity)
                }
                
                when (result) {
                    is UseCaseResult.Success -> {
                        setState { copy(isSubmitting = false) }
                        sendEffect { SubmitCatchEffect.CatchSubmittedSuccessfully }
                    }
                    is UseCaseResult.Error -> {
                        setState { copy(isSubmitting = false) }
                        sendEffect { SubmitCatchEffect.ShowError(result.message) }
                    }
                }
            } catch (e: Exception) {
                logError("Failed to submit catch", e)
                setState { copy(isSubmitting = false) }
                sendEffect { SubmitCatchEffect.ShowError("Failed to submit catch: ${e.message}") }
            }
        }
    }

    override fun createInitialState(): SubmitCatchState {
        return SubmitCatchState()
    }
}