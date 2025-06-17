package com.hooked.core.photo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

actual class PhotoCapture(
    private val activity: ComponentActivity,
    private val launchers: PhotoLaunchers
) {
    
    private val imageProcessor = ImageProcessor(activity)
    val permissionFlow = MutableStateFlow(false)
    
    var currentPhotoUri: Uri? = null
    var captureCallback: ((PhotoCaptureResult) -> Unit)? = null
    
    companion object {
        var instance: PhotoCapture? = null
    }
    
    init {
        instance = this
    }
    
    actual suspend fun capturePhoto(): PhotoCaptureResult {
        if (!hasRequiredPermissions()) {
            return PhotoCaptureResult.Error("Camera and location permissions required")
        }
        
        return suspendCancellableCoroutine { continuation ->
            captureCallback = { result ->
                captureCallback = null
                continuation.resume(result)
            }
            
            try {
                val photoFile = createImageFile()
                currentPhotoUri = FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.fileprovider",
                    photoFile
                )
                currentPhotoUri?.let { uri ->
                    launchers.cameraLauncher.launch(uri)
                } ?: captureCallback?.invoke(PhotoCaptureResult.Error("Failed to create photo URI"))
            } catch (e: Exception) {
                captureCallback?.invoke(PhotoCaptureResult.Error("Failed to start camera: ${e.message}"))
            }
        }
    }
    
    actual suspend fun pickFromGallery(): PhotoCaptureResult {
        if (!hasStoragePermission()) {
            return PhotoCaptureResult.Error("Storage permission required")
        }
        
        return suspendCancellableCoroutine { continuation ->
            captureCallback = { result ->
                captureCallback = null
                continuation.resume(result)
            }
            
            try {
                launchers.galleryLauncher.launch("image/*")
            } catch (e: Exception) {
                captureCallback?.invoke(PhotoCaptureResult.Error("Failed to open gallery: ${e.message}"))
            }
        }
    }
    
    actual fun requestPermissions(): Flow<Boolean> {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES
        )
        
        if (hasRequiredPermissions()) {
            permissionFlow.value = true
        } else {
            launchers.permissionLauncher.launch(permissions)
        }
        
        return permissionFlow
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return hasCameraPermission() && hasLocationPermission() && hasStoragePermission()
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            activity, Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun createImageFile(): File {
        val timestamp = System.currentTimeMillis()
        val fileName = "CATCH_${timestamp}.jpg"
        return File(activity.getExternalFilesDir(null), fileName)
    }
    
    fun handleCameraResult(uri: Uri) {
        try {
            val capturedPhoto = CapturedPhoto(
                imageUri = uri.toString(),
                metadata = null
            )
            
            captureCallback?.invoke(PhotoCaptureResult.Success(capturedPhoto))
        } catch (e: Exception) {
            captureCallback?.invoke(PhotoCaptureResult.Error("Failed to process camera image: ${e.message}"))
        }
    }
    
    fun handleGalleryResult(uri: Uri) {
        try {
            val capturedPhoto = CapturedPhoto(
                imageUri = uri.toString(),
                metadata = null
            )
            
            captureCallback?.invoke(PhotoCaptureResult.Success(capturedPhoto))
        } catch (e: Exception) {
            captureCallback?.invoke(PhotoCaptureResult.Error("Failed to process gallery image: ${e.message}"))
        }
    }
}