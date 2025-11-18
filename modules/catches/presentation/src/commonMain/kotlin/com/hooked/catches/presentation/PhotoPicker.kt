package com.hooked.catches.presentation

import androidx.compose.runtime.Composable

/**
 * Platform-specific photo picker interface.
 * Each platform provides its own implementation for launching the photo picker.
 */
interface PhotoPicker {
    @Composable
    fun rememberPhotoPickerLauncher(onPhotoSelected: (String) -> Unit): () -> Unit
}

/**
 * Expected to be provided by each platform's dependency injection
 */
expect fun getPhotoPicker(): PhotoPicker