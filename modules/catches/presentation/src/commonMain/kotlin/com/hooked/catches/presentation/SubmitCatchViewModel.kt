package com.hooked.catches.presentation

import com.hooked.core.HookedViewModel
import com.hooked.catches.domain.entities.SubmitCatchEntity
import com.hooked.catches.domain.usecases.SubmitCatchUseCase
import com.hooked.core.photo.ImageProcessor
import com.hooked.core.photo.encodeBase64

import com.hooked.core.domain.UseCaseResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.hooked.catches.presentation.model.SubmitCatchEffect
import com.hooked.catches.presentation.model.SubmitCatchIntent
import com.hooked.catches.presentation.model.SubmitCatchState
import com.hooked.core.logging.logError

class SubmitCatchViewModel(
    private val submitCatchUseCase: SubmitCatchUseCase,
    private val imageProcessor: ImageProcessor,
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
            is SubmitCatchIntent.RemovePhoto -> {
                setState { copy(photoUri = null) }
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
            sendEffect { SubmitCatchEffect.ShowError("Please select a photo") }
            return
        }

        setState { copy(isSubmitting = true) }
        
        viewModelScope.launch {
            try {
                // Convert image to base64
                val imageBase64 = currentState.photoUri?.let {
                    try {
                        val imageBytes = imageProcessor.loadImageFromUri(it)
                        imageBytes.encodeBase64()
                    } catch (e: Exception) {
                        logError("Failed to convert image to base64", e)
                        null // Continue with submission even if base64 conversion fails
                    }
                }
                
                val catchEntity = SubmitCatchEntity(
                    species = currentState.species,
                    location = null, // Will be enriched by backend
                    latitude = currentState.latitude,
                    longitude = currentState.longitude,
                    caughtAt = null, // Will be extracted from EXIF by backend
                    notes = null, // No notes field in current UI
                    imageBase64 = imageBase64
                )
                
                when (val result = submitCatchUseCase(catchEntity)) {
                    is UseCaseResult.Success -> {
                        setState { 
                            copy(
                                isSubmitting = false,
                                submittedCatchId = result.data
                            )
                        }
                        
                        delay(150)
                        
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