package com.hooked.features.submit.presentation.model

sealed class SubmitCatchEffect {
    object NavigateBack : SubmitCatchEffect()
    data class ShowError(val message: String) : SubmitCatchEffect()
    object ShowPhotoPickerDialog : SubmitCatchEffect()
    object TakePhoto : SubmitCatchEffect()
    object PickPhotoFromGallery : SubmitCatchEffect()
    object RequestLocationPermission : SubmitCatchEffect()
    object CatchSubmittedSuccessfully : SubmitCatchEffect()
}