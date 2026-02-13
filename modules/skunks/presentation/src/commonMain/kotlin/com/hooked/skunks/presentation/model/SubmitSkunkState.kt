package com.hooked.skunks.presentation.model

data class SubmitSkunkState(
    val fishedAt: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String = "",
    val notes: String = "",
    val isLocationLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val errorMessage: String? = null
) {
    val isValid: Boolean get() = fishedAt.isNotBlank()
    val hasLocation: Boolean get() = latitude != null && longitude != null
}
