package com.hooked.submit.domain.usecases

import com.hooked.core.domain.UseCaseResult
import com.hooked.core.photo.ImageProcessor
import com.hooked.core.photo.encodeBase64

class ConvertImageToBytesUseCase(
    private val imageProcessor: ImageProcessor
) {
    suspend operator fun invoke(imageUri: String): UseCaseResult<ByteArray> {
        return try {
            val imageBytes = imageProcessor.loadImageFromUri(imageUri)
            val processedBytes = imageProcessor.processImageWithExif(imageBytes)
            UseCaseResult.Success(processedBytes)
        } catch (e: Exception) {
            UseCaseResult.Error(
                "Failed to process image: ${e.message}",
                "ConvertImageToBytesUseCase"
            )
        }
    }
    
    suspend fun convertToBase64(imageUri: String): UseCaseResult<String> {
        return try {
            val imageBytes = imageProcessor.loadImageFromUri(imageUri)
            val processedBytes = imageProcessor.processImageWithExif(imageBytes)
            val base64String = processedBytes.encodeBase64()
            UseCaseResult.Success(base64String)
        } catch (e: Exception) {
            UseCaseResult.Error(
                "Failed to convert image to base64: ${e.message}",
                "ConvertImageToBytesUseCase"
            )
        }
    }
}