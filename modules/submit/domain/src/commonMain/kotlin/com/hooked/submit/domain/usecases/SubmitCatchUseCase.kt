package com.hooked.submit.domain.usecases

import com.hooked.submit.domain.entities.SubmitCatchEntity
import com.hooked.submit.domain.repositories.SubmitRepository

class SubmitCatchUseCase(private val submitRepository: SubmitRepository) {
    suspend operator fun invoke(catchEntity: SubmitCatchEntity): SubmitCatchUseCaseResult {
        return try {
            val result = submitRepository.submitCatch(catchEntity)
            if (result.isSuccess) {
                result.getOrNull()?.let { catchId ->
                    SubmitCatchUseCaseResult.Success(catchId)
                } ?: SubmitCatchUseCaseResult.Error("Failed to submit catch")
            } else {
                SubmitCatchUseCaseResult.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            SubmitCatchUseCaseResult.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class SubmitCatchUseCaseResult {
    data class Success(val catchId: Long) : SubmitCatchUseCaseResult()
    data class Error(val message: String) : SubmitCatchUseCaseResult()
}