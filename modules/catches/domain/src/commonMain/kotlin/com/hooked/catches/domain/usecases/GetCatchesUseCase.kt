package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.repositories.CatchRepository

class GetCatchesUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(): GetCatchesUseCaseResult {
        return try {
            val result = catchRepository.getCatches()
            if (result.isSuccess) {
                GetCatchesUseCaseResult.Success(result.getOrNull() ?: emptyList())
            } else {
                GetCatchesUseCaseResult.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            GetCatchesUseCaseResult.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class GetCatchesUseCaseResult {
    data class Success(val catches: List<CatchEntity>) : GetCatchesUseCaseResult()
    data class Error(val message: String) : GetCatchesUseCaseResult()
}