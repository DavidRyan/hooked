package com.hooked.features.submit.presentation.model

sealed class SubmitCatchIntent {
    data class UpdateSpecies(val species: String) : SubmitCatchIntent()
    data class UpdateWeight(val weight: String) : SubmitCatchIntent()
    data class UpdateLength(val length: String) : SubmitCatchIntent()
    data class UpdateLocation(val latitude: Double, val longitude: Double) : SubmitCatchIntent()
    data class UpdatePhoto(val photoUri: String?) : SubmitCatchIntent()
    object TakePhoto : SubmitCatchIntent()
    object PickPhoto : SubmitCatchIntent()
    object GetCurrentLocation : SubmitCatchIntent()
    object SubmitCatch : SubmitCatchIntent()
    object NavigateBack : SubmitCatchIntent()
}