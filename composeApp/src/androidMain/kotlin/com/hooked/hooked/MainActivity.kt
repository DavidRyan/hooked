package com.hooked.hooked

import android.Manifest
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import di.initKoin
import com.hooked.HookedApp
import com.hooked.core.photo.PhotoLaunchers
import com.hooked.core.photo.PhotoCaptureResult
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {
    
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register activity result launchers early
        initializeLaunchers()
        
        initKoin {
            androidContext(this@MainActivity)
        }
        
        // Provide launchers to DI after Koin is initialized
        GlobalContext.get().declare(PhotoLaunchers(cameraLauncher, galleryLauncher, permissionLauncher))
        
        setContent {
            HookedApp()
        }
    }
    
    private fun initializeLaunchers() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            com.hooked.core.photo.PhotoCapture.instance?.let { photoCapture ->
                if (success && photoCapture.currentPhotoUri != null) {
                    photoCapture.handleCameraResult(photoCapture.currentPhotoUri!!)
                } else {
                    photoCapture.captureCallback?.invoke(PhotoCaptureResult.Cancelled)
                }
            }
        }
        
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            com.hooked.core.photo.PhotoCapture.instance?.let { photoCapture ->
                if (uri != null) {
                    photoCapture.handleGalleryResult(uri)
                } else {
                    photoCapture.captureCallback?.invoke(PhotoCaptureResult.Cancelled)
                }
            }
        }
        
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            com.hooked.core.photo.PhotoCapture.instance?.let { photoCapture ->
                val allGranted = permissions.values.all { it }
                photoCapture.permissionFlow.value = allGranted
            }
        }
    }
}
