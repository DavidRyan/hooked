package com.hooked.submit.domain.usecases

import com.hooked.core.domain.UseCaseResult
import com.hooked.core.photo.ImageProcessor

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
    
}
