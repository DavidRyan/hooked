package com.hooked.submit.presentation

import com.hooked.core.HookedViewModel
import com.hooked.core.photo.ImageProcessor
import com.hooked.core.photo.PhotoCapture
import com.hooked.core.photo.PhotoCaptureResult
import com.hooked.core.photo.encodeBase64
import com.hooked.submit.data.model.SubmitCatchRequest
import com.hooked.submit.data.usecases.SubmitCatchUseCase
import com.hooked.submit.data.usecases.SubmitCatchUseCaseResult
import kotlinx.coroutines.launch
import com.hooked.submit.presentation.model.SubmitCatchEffect
import com.hooked.submit.presentation.model.SubmitCatchIntent
import com.hooked.submit.presentation.model.SubmitCatchState

class SubmitCatchViewModel(
    private val submitCatchUseCase: SubmitCatchUseCase,
    private val photoCapture: PhotoCapture,
    private val imageProcessor: ImageProcessor
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
            is SubmitCatchIntent.TakePhoto -> {
                capturePhoto()
            }
            is SubmitCatchIntent.PickPhoto -> {
                pickPhoto()
            }
            is SubmitCatchIntent.GetCurrentLocation -> {
                setState { copy(isLocationLoading = true) }
                sendEffect { SubmitCatchEffect.RequestLocationPermission }
                // Location update will come through UpdateLocation intent
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
                val request = SubmitCatchRequest(
                    species = currentState.species,
                    weight = currentState.weight.toDouble(),
                    length = currentState.length.toDouble(),
                    latitude = currentState.latitude,
                    longitude = currentState.longitude,
                    photoBase64 = currentState.photoUri?.let { convertImageToBase64(it) }
                )
                
                when (val result = submitCatchUseCase(request)) {
                    is SubmitCatchUseCaseResult.Success -> {
                        setState { copy(isSubmitting = false) }
                        sendEffect { SubmitCatchEffect.CatchSubmittedSuccessfully }
                    }
                    is SubmitCatchUseCaseResult.Error -> {
                        setState { copy(isSubmitting = false) }
                        sendEffect { SubmitCatchEffect.ShowError(result.message) }
                    }
                }
            } catch (e: Exception) {
                setState { copy(isSubmitting = false) }
                sendEffect { SubmitCatchEffect.ShowError("Failed to submit catch: ${e.message}") }
            }
        }
    }

    private fun capturePhoto() {
        viewModelScope.launch {
            when (val result = photoCapture.capturePhoto()) {
                is PhotoCaptureResult.Success -> {
                    setState { copy(photoUri = result.photo.imageUri) }
                }
                is PhotoCaptureResult.Error -> {
                    sendEffect { SubmitCatchEffect.ShowError(result.message) }
                }
                PhotoCaptureResult.Cancelled -> {
                    // User cancelled, no action needed
                }
            }
        }
    }
    
    private fun pickPhoto() {
        viewModelScope.launch {
            when (val result = photoCapture.pickFromGallery()) {
                is PhotoCaptureResult.Success -> {
                    setState { copy(photoUri = result.photo.imageUri) }
                }
                is PhotoCaptureResult.Error -> {
                    sendEffect { SubmitCatchEffect.ShowError(result.message) }
                }
                PhotoCaptureResult.Cancelled -> {
                    // User cancelled, no action needed
                }
            }
        }
    }

    private suspend fun convertImageToBase64(imageUri: String): String {
        return try {
            // Load image bytes with EXIF data preserved
            val imageBytes = imageProcessor.loadImageFromUri(imageUri)
            
            // Process image while preserving EXIF metadata
            val processedBytes = imageProcessor.processImageWithExif(imageBytes)
            
            // Convert to Base64 with EXIF intact for backend processing
            processedBytes.encodeBase64()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to process image: ${e.message}")
        }
    }

    override fun createInitialState(): SubmitCatchState {
        return SubmitCatchState()
    }
}