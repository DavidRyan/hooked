package com.hooked.catches.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

class AndroidPhotoPicker : PhotoPicker {
    @Composable
    override fun rememberPhotoPickerLauncher(onPhotoSelected: (String) -> Unit): () -> Unit {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.toString()?.let(onPhotoSelected)
        }
        
        return { launcher.launch("image/*") }
    }
}

actual fun getPhotoPicker(): PhotoPicker = AndroidPhotoPicker()