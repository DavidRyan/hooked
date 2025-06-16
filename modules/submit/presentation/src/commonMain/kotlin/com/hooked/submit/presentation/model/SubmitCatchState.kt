package com.hooked.features.submit.presentation.model

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
        get() = species.isNotBlank() && 
                weight.toDoubleOrNull() != null && 
                length.toDoubleOrNull() != null
                
    val locationText: String
        get() = if (latitude != null && longitude != null) {
            "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
        } else {
            "No location set"
        }
}