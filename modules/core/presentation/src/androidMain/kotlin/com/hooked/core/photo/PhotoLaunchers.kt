package com.hooked.core.photo

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

data class PhotoLaunchers(
    val cameraLauncher: ActivityResultLauncher<Uri>,
    val galleryLauncher: ActivityResultLauncher<String>,
    val permissionLauncher: ActivityResultLauncher<Array<String>>
)