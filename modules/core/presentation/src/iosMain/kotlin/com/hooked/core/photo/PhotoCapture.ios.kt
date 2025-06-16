package com.hooked.core.photo

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.*
import platform.CoreLocation.*
import platform.Foundation.*
import platform.Photos.*
import platform.UIKit.*
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)

actual class PhotoCapture {
    
    private val imageProcessor = ImageProcessor()
    private val permissionFlow = MutableStateFlow(false)
    
    actual suspend fun capturePhoto(): PhotoCaptureResult {
        if (!hasRequiredPermissions()) {
            return PhotoCaptureResult.Error("Camera and location permissions required")
        }
        
        // TODO: Implement proper camera capture with UIImagePickerController
        return PhotoCaptureResult.Error("Camera capture not yet implemented on iOS")
    }
    
    actual suspend fun pickFromGallery(): PhotoCaptureResult {
        if (!hasPhotoLibraryPermission()) {
            return PhotoCaptureResult.Error("Photo library permission required")
        }
        
        // TODO: Implement proper photo picker with UIImagePickerController
        return PhotoCaptureResult.Error("Photo picker not yet implemented on iOS")
    }
    
    actual fun requestPermissions(): Flow<Boolean> {
        requestCameraPermission()
        requestLocationPermission()
        requestPhotoLibraryPermission()
        
        return permissionFlow
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return hasCameraPermission() && hasLocationPermission()
    }
    
    private fun hasCameraPermission(): Boolean {
        return AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == 
               AVAuthorizationStatusAuthorized
    }
    
    private fun hasLocationPermission(): Boolean {
        val status = CLLocationManager().authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedWhenInUse || 
               status == kCLAuthorizationStatusAuthorizedAlways
    }
    
    private fun hasPhotoLibraryPermission(): Boolean {
        return PHPhotoLibrary.authorizationStatus() == PHAuthorizationStatusAuthorized
    }
    
    private fun requestCameraPermission() {
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
            checkAllPermissions()
        }
    }
    
    private fun requestLocationPermission() {
        val locationManager = CLLocationManager()
        locationManager.requestWhenInUseAuthorization()
        checkAllPermissions()
    }
    
    private fun requestPhotoLibraryPermission() {
        PHPhotoLibrary.requestAuthorization { status ->
            checkAllPermissions()
        }
    }
    
    private fun checkAllPermissions() {
        val allGranted = hasCameraPermission() && hasLocationPermission() && hasPhotoLibraryPermission()
        permissionFlow.value = allGranted
    }
    
}