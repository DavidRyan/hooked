package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.entities.FishingInsightsEntity
import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.core.domain.UseCaseResult

class GetFishingInsightsUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(): UseCaseResult<FishingInsightsEntity> {
        return try {
            val result = catchRepository.getFishingInsights()
            UseCaseResult.fromResult(
                result,
                result.exceptionOrNull()?.message ?: "Failed to load fishing insights",
                result.exceptionOrNull(),
                "GetFishingInsightsUseCase"
            )
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Unknown error", e, "GetFishingInsightsUseCase")
        }
    }
}
