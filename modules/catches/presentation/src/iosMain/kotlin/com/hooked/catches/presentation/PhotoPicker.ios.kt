package com.hooked.catches.presentation

import androidx.compose.runtime.Composable
import com.hooked.core.logging.Logger

class IosPhotoPicker : PhotoPicker {
    companion object {
        private const val TAG = "IosPhotoPicker"
    }
    
    @Composable
    override fun rememberPhotoPickerLauncher(onPhotoSelected: (String) -> Unit): () -> Unit {
        // For now, return a stub function
        // iOS implementation would involve UIImagePickerController
        // This would need platform-specific code to actually work
        return { 
            // TODO: Implement iOS photo picker
            Logger.warning(TAG, "iOS photo picker not yet implemented")
        }
    }
}

actual fun getPhotoPicker(): PhotoPicker = IosPhotoPicker()