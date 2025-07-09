package com.hooked.submit.domain.usecases

import com.hooked.core.domain.UseCaseResult
import com.hooked.core.photo.ImageProcessor
import com.hooked.core.photo.encodeBase64
import com.hooked.core.logging.logError

class ConvertImageToBase64UseCase(
    private val imageProcessor: ImageProcessor
) {
    suspend operator fun invoke(imageUri: String): UseCaseResult<String> {
        return try {
            val imageBytes = imageProcessor.loadImageFromUri(imageUri)
            val processedBytes = imageProcessor.processImageWithExif(imageBytes)
            val base64String = processedBytes.encodeBase64()
            
            UseCaseResult.Success(base64String)
        } catch (e: Exception) {
            logError("Failed to convert image to base64", e)
            UseCaseResult.Error("Failed to process image: ${e.message}")
        }
    }
}