package submit

import core.HookedViewModel
import domain.model.SubmitCatchRequest
import domain.usecase.SubmitCatchUseCase
import domain.usecase.SubmitCatchUseCaseResult
import kotlinx.coroutines.launch
import submit.model.SubmitCatchEffect
import submit.model.SubmitCatchIntent
import submit.model.SubmitCatchState

class SubmitCatchViewModel(
    private val submitCatchUseCase: SubmitCatchUseCase
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
                sendEffect { SubmitCatchEffect.TakePhoto }
            }
            is SubmitCatchIntent.PickPhoto -> {
                sendEffect { SubmitCatchEffect.PickPhotoFromGallery }
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

    private suspend fun convertImageToBase64(imageUri: String): String {
        // This will be implemented in platform-specific code
        // IMPORTANT: Preserve EXIF metadata including location and timestamp
        return imageUri
    }

    override fun createInitialState(): SubmitCatchState {
        return SubmitCatchState()
    }
}