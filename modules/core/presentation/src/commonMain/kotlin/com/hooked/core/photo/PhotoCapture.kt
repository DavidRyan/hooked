package com.hooked.core.photo

import com.hooked.core.logging.Logger
import kotlinx.coroutines.flow.Flow

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null
)

data class CameraInfo(
    val make: String? = null,
    val model: String? = null,
    val orientation: Int? = null
)

data class PhotoMetadata(
    val location: LocationData? = null,
    val timestamp: Long? = null,
)

data class CapturedPhoto(
    val imageUri: String,
    val metadata: PhotoMetadata? = null
)

sealed class PhotoCaptureResult {
    data class Success(val photo: CapturedPhoto) : PhotoCaptureResult()
    data class Error(val message: String, val context: String? = null) : PhotoCaptureResult() {
        init {
            val tag = context ?: "PhotoCapture"
            Logger.error(tag, message)
        }
    }
    object Cancelled : PhotoCaptureResult()
}

expect class PhotoCapture {
    suspend fun capturePhoto(): PhotoCaptureResult
    suspend fun pickFromGallery(): PhotoCaptureResult
    fun requestPermissions(): Flow<Boolean>
}