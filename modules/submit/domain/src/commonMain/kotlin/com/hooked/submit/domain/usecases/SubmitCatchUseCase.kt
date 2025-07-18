package com.hooked.submit.domain.usecases

import com.hooked.submit.domain.entities.SubmitCatchEntity
import com.hooked.submit.domain.repositories.SubmitRepository
import com.hooked.core.domain.UseCaseResult

class SubmitCatchUseCase(private val submitRepository: SubmitRepository) {
    suspend operator fun invoke(catchEntity: SubmitCatchEntity): UseCaseResult<String> {
        return try {
            val result = submitRepository.submitCatch(catchEntity)
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