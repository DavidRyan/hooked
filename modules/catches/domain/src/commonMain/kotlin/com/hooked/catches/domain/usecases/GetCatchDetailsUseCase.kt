package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.entities.CatchDetailsEntity
import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.core.domain.UseCaseResult

class GetCatchDetailsUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(catchId: String): UseCaseResult<CatchDetailsEntity> {
        return try {
            val result = catchRepository.getCatchDetails(catchId)
            if (result.isSuccess) {
                result.getOrNull()?.let { catchDetails ->
                    UseCaseResult.Success(catchDetails)
                } ?: UseCaseResult.Error("Catch details not found", context = "GetCatchDetailsUseCase")
            } else {
                val exception = result.exceptionOrNull()
                UseCaseResult.Error(
                    exception?.message ?: "Unknown error",
                    exception,
                    "GetCatchDetailsUseCase"
                )
            }
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Unknown error", e, "GetCatchDetailsUseCase")
        }
    }
}