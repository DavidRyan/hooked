package com.hooked.core.photo

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

data class PhotoLaunchers(
    val cameraLauncher: ActivityResultLauncher<Uri>,
    // OpenDocument takes Array<String> (MIME types) to preserve EXIF metadata including GPS location
    val galleryLauncher: ActivityResultLauncher<Array<String>>,
    val permissionLauncher: ActivityResultLauncher<Array<String>>
)