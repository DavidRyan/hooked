package com.hooked.skunks.presentation.model

sealed class SubmitSkunkIntent {
    data class UpdateFishedAt(val fishedAt: String) : SubmitSkunkIntent()
    data class UpdateNotes(val notes: String) : SubmitSkunkIntent()
    data class UpdateLocationFromMap(val latitude: Double, val longitude: Double) : SubmitSkunkIntent()
    data object GetCurrentLocation : SubmitSkunkIntent()
    data object Submit : SubmitSkunkIntent()
    data object DismissError : SubmitSkunkIntent()
}
