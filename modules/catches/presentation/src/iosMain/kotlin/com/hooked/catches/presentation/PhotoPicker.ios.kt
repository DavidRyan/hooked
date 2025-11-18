package com.hooked.catches.presentation

import androidx.compose.runtime.Composable

class IosPhotoPicker : PhotoPicker {
    @Composable
    override fun rememberPhotoPickerLauncher(onPhotoSelected: (String) -> Unit): () -> Unit {
        // For now, return a stub function
        // iOS implementation would involve UIImagePickerController
        // This would need platform-specific code to actually work
        return { 
            // TODO: Implement iOS photo picker
            println("iOS photo picker not yet implemented")
        }
    }
}

actual fun getPhotoPicker(): PhotoPicker = IosPhotoPicker()