package com.hooked.catches.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

class AndroidPhotoPicker : PhotoPicker {
    @Composable
    override fun rememberPhotoPickerLauncher(onPhotoSelected: (String) -> Unit): () -> Unit {
        // Use OpenDocument instead of GetContent to preserve EXIF metadata (including GPS location)
        // The photo picker and GetContent strip location data for privacy, but OpenDocument
        // provides access to the original file with all metadata intact
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.toString()?.let(onPhotoSelected)
        }
        
        return { launcher.launch(arrayOf("image/*")) }
    }
}

actual fun getPhotoPicker(): PhotoPicker = AndroidPhotoPicker()