package core.photo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.*
import platform.Foundation.*
import platform.Photos.*
import platform.UIKit.*
import kotlin.coroutines.resume

actual class PhotoCapture {
    
    private val imageProcessor = ImageProcessor()
    private val permissionFlow = MutableStateFlow(false)
    
    actual suspend fun capturePhoto(): PhotoCaptureResult {
        if (!hasRequiredPermissions()) {
            return PhotoCaptureResult.Error("Camera and location permissions required")
        }
        
        return suspendCancellableCoroutine { continuation ->
            val picker = UIImagePickerController().apply {
                sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                allowsEditing = false
                delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
                    override fun imagePickerController(
                        picker: UIImagePickerController,
                        didFinishPickingMediaWithInfo: Map<Any?, *>
                    ) {
                        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                        picker.dismissViewControllerAnimated(true, null)
                        
                        if (image != null) {
                            handleImageResult(image) { result ->
                                continuation.resume(result)
                            }
                        } else {
                            continuation.resume(PhotoCaptureResult.Error("Failed to capture image"))
                        }
                    }
                    
                    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                        picker.dismissViewControllerAnimated(true, null)
                        continuation.resume(PhotoCaptureResult.Cancelled)
                    }
                }
            }
            
            UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
                picker, animated = true, completion = null
            )
        }
    }
    
    actual suspend fun pickFromGallery(): PhotoCaptureResult {
        if (!hasPhotoLibraryPermission()) {
            return PhotoCaptureResult.Error("Photo library permission required")
        }
        
        return suspendCancellableCoroutine { continuation ->
            val picker = UIImagePickerController().apply {
                sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                allowsEditing = false
                delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
                    override fun imagePickerController(
                        picker: UIImagePickerController,
                        didFinishPickingMediaWithInfo: Map<Any?, *>
                    ) {
                        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                        picker.dismissViewControllerAnimated(true, null)
                        
                        if (image != null) {
                            handleImageResult(image) { result ->
                                continuation.resume(result)
                            }
                        } else {
                            continuation.resume(PhotoCaptureResult.Error("Failed to select image"))
                        }
                    }
                    
                    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                        picker.dismissViewControllerAnimated(true, null)
                        continuation.resume(PhotoCaptureResult.Cancelled)
                    }
                }
            }
            
            UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
                picker, animated = true, completion = null
            )
        }
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
               AVAuthorizationStatus.AVAuthorizationStatusAuthorized
    }
    
    private fun hasLocationPermission(): Boolean {
        val status = CLLocationManager().authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedWhenInUse || 
               status == kCLAuthorizationStatusAuthorizedAlways
    }
    
    private fun hasPhotoLibraryPermission(): Boolean {
        return PHPhotoLibrary.authorizationStatus() == PHAuthorizationStatus.PHAuthorizationStatusAuthorized
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
    
    private fun handleImageResult(image: UIImage, callback: (PhotoCaptureResult) -> Unit) {
        try {
            // Convert UIImage to JPEG with EXIF preserved
            val imageData = UIImageJPEGRepresentation(image, 0.85)
            if (imageData != null) {
                // Save to temporary file and return URI
                val tempUrl = saveTempImage(imageData)
                val capturedPhoto = CapturedPhoto(
                    imageUri = tempUrl,
                    metadata = null // Backend will extract from image
                )
                callback(PhotoCaptureResult.Success(capturedPhoto))
            } else {
                callback(PhotoCaptureResult.Error("Failed to convert image"))
            }
        } catch (e: Exception) {
            callback(PhotoCaptureResult.Error("Failed to process image: ${e.message}"))
        }
    }
    
    private fun saveTempImage(imageData: NSData): String {
        val tempDir = NSTemporaryDirectory()
        val filename = "CATCH_${NSDate().timeIntervalSince1970}.jpg"
        val tempPath = "$tempDir/$filename"
        
        imageData.writeToFile(tempPath, atomically = true)
        return tempPath
    }
}