package com.hooked.catches.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.kmpicture.ui.ImageSelectorBottomSheet

class AndroidPhotoPicker : PhotoPicker {
    @Composable
    override fun rememberPhotoPickerLauncher(onPhotoSelected: (String) -> Unit): () -> Unit {
        var showPicker by remember { mutableStateOf(false) }

        ImageSelectorBottomSheet(
            visible = showPicker,
            onDismiss = { showPicker = false },
            onSelected = { selection ->
                selection.uri?.toString()?.let(onPhotoSelected)
                showPicker = false
            }
        )

        return { showPicker = true }
    }
}

actual fun getPhotoPicker(): PhotoPicker = AndroidPhotoPicker()
