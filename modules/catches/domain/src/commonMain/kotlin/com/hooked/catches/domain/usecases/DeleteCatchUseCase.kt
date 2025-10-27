package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.core.domain.UseCaseResult

class DeleteCatchUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(catchId: String): UseCaseResult<Unit> {
        return try {
            val result = catchRepository.deleteCatch(catchId)
            if (result.isSuccess) {
                UseCaseResult.Success(Unit)
            } else {
                val exception = result.exceptionOrNull()
                UseCaseResult.Error(
                    exception?.message ?: "Failed to delete catch",
                    exception,
                    "DeleteCatchUseCase"
                )
            }
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Unknown error", e, "DeleteCatchUseCase")
        }
    }
}
