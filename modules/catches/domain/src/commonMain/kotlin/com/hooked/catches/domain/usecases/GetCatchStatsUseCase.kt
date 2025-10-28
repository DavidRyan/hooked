package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.entities.StatsEntity
import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.core.domain.UseCaseResult

class GetCatchStatsUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(): UseCaseResult<StatsEntity> {
        return try {
            val result = catchRepository.getCatchStats()
            if (result.isSuccess) {
                UseCaseResult.Success(result.getOrNull()!!)
            } else {
                val exception = result.exceptionOrNull()
                UseCaseResult.Error(
                    exception?.message ?: "Failed to load stats",
                    exception,
                    "GetCatchStatsUseCase"
                )
            }
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Unknown error", e, "GetCatchStatsUseCase")
        }
    }
}