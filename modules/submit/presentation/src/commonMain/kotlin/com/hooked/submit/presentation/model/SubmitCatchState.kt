package com.hooked.submit.presentation.model

data class SubmitCatchState(
    val species: String = "",
    val weight: String = "",
    val length: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoUri: String? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isLocationLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isFormValid: Boolean
        get() = photoUri != null
                
    val locationText: String
        get() = if (latitude != null && longitude != null) {
            "${latitude.toString().take(8)}, ${longitude.toString().take(8)}"
        } else {
            "No location set"
        }
}