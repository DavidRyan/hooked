package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.entities.SubmitCatchEntity
import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.core.domain.UseCaseResult

class SubmitCatchUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(catchEntity: SubmitCatchEntity): UseCaseResult<String> {
        return try {
            val result = catchRepository.submitCatch(catchEntity)
            if (result.isSuccess) {
                result.getOrNull()?.let { catchId ->
                    UseCaseResult.Success(catchId)
                } ?: UseCaseResult.Error("Failed to submit catch - no catch ID returned", context = "SubmitCatchUseCase")
            } else {
                val exception = result.exceptionOrNull()
                UseCaseResult.Error(
                    exception?.message ?: "Unknown error", 
                    exception,
                    "SubmitCatchUseCase"
                )
            }
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Unknown error", e, "SubmitCatchUseCase")
        }
    }
}