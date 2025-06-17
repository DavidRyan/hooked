package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.core.domain.UseCaseResult

class GetCatchesUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(): UseCaseResult<List<CatchEntity>> {
        return try {
            val result = catchRepository.getCatches()
            if (result.isSuccess) {
                UseCaseResult.Success(result.getOrNull() ?: emptyList())
            } else {
                val exception = result.exceptionOrNull()
                UseCaseResult.Error(
                    exception?.message ?: "Unknown error",
                    exception,
                    "GetCatchesUseCase"
                )
            }
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Unknown error", e, "GetCatchesUseCase")
        }
    }
}