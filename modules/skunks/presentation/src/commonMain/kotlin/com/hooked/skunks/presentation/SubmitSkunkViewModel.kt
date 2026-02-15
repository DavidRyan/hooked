package com.hooked.skunks.presentation

import com.hooked.core.HookedViewModel
import com.hooked.core.location.LocationResult
import com.hooked.core.location.LocationService
import com.hooked.skunks.domain.entities.SubmitSkunkEntity
import com.hooked.skunks.domain.usecases.SubmitSkunkUseCase
import com.hooked.skunks.presentation.model.SubmitSkunkEffect
import com.hooked.skunks.presentation.model.SubmitSkunkIntent
import com.hooked.skunks.presentation.model.SubmitSkunkState
import kotlinx.coroutines.launch

class SubmitSkunkViewModel(
    private val submitSkunkUseCase: SubmitSkunkUseCase,
    private val locationService: LocationService
) : HookedViewModel<SubmitSkunkIntent, SubmitSkunkState, SubmitSkunkEffect>() {

    override fun createInitialState(): SubmitSkunkState {
        // Note: Using empty string as placeholder for initial date
        // The UI should set the actual date via UpdateFishedAt intent
        val hasPermission = try {
            locationService.hasLocationPermission()
        } catch (e: Exception) {
            false
        }

        return SubmitSkunkState(
            fishedAt = "", // Will be set by the UI
            hasLocationPermission = hasPermission
        )
    }

    override fun handleIntent(intent: SubmitSkunkIntent) {
        when (intent) {
            is SubmitSkunkIntent.UpdateFishedAt -> {
                setState { copy(fishedAt = intent.fishedAt) }
            }

            is SubmitSkunkIntent.UpdateNotes -> {
                setState { copy(notes = intent.notes) }
            }

            is SubmitSkunkIntent.UpdateLocationFromMap -> {
                val latStr = ((intent.latitude * 10000).toLong() / 10000.0).toString()
                val lngStr = ((intent.longitude * 10000).toLong() / 10000.0).toString()
                setState {
                    copy(
                        latitude = intent.latitude,
                        longitude = intent.longitude,
                        locationName = "$latStr, $lngStr"
                    )
                }
            }

            is SubmitSkunkIntent.GetCurrentLocation -> {
                getCurrentLocation()
            }

            is SubmitSkunkIntent.Submit -> {
                submitSkunk()
            }

            is SubmitSkunkIntent.DismissError -> {
                setState { copy(errorMessage = null) }
            }
        }
    }

    private fun getCurrentLocation() {
        if (!locationService.hasLocationPermission()) {
            sendEffect { SubmitSkunkEffect.RequestLocationPermission }
            return
        }

        setState { copy(isLocationLoading = true) }

        viewModelScope.launch {
            when (val result = locationService.getCurrentLocation()) {
                is LocationResult.Success -> {
                    val latStr = ((result.latitude * 10000).toLong() / 10000.0).toString()
                    val lngStr = ((result.longitude * 10000).toLong() / 10000.0).toString()
                    setState {
                        copy(
                            latitude = result.latitude,
                            longitude = result.longitude,
                            locationName = "$latStr, $lngStr",
                            isLocationLoading = false
                        )
                    }
                }

                is LocationResult.Error -> {
                    setState {
                        copy(
                            isLocationLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun submitSkunk() {
        val currentState = state.value

        if (!currentState.isValid) {
            setState { copy(errorMessage = "Please set when you fished") }
            return
        }

        setState { copy(isSubmitting = true) }

        viewModelScope.launch {
            try {
                val entity = SubmitSkunkEntity(
                    latitude = currentState.latitude,
                    longitude = currentState.longitude,
                    fishedAt = currentState.fishedAt,
                    notes = currentState.notes.ifBlank { null }
                )

                val result = submitSkunkUseCase(entity)

                result.fold(
                    onSuccess = {
                        setState { copy(isSubmitting = false, isSubmitted = true) }
                        sendEffect { SubmitSkunkEffect.SubmitSuccess }
                    },
                    onFailure = { e ->
                        setState {
                            copy(
                                isSubmitting = false,
                                errorMessage = "Failed to log skunk: ${e.message}"
                            )
                        }
                        sendEffect {
                            SubmitSkunkEffect.SubmitError(
                                e.message ?: "Unknown error"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                setState {
                    copy(
                        isSubmitting = false,
                        errorMessage = "Failed to log skunk: ${e.message}"
                    )
                }
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        setState { copy(hasLocationPermission = granted) }
        if (granted) {
            getCurrentLocation()
        }
    }

    fun requestLocationPermission() {
        viewModelScope.launch {
            locationService.requestLocationPermission()
            setState { copy(hasLocationPermission = locationService.hasLocationPermission()) }
        }
    }
}
