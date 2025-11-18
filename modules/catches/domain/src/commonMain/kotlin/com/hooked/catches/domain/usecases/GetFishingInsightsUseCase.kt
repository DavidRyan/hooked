package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.entities.FishingInsightsEntity
import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.core.domain.UseCaseResult

class GetFishingInsightsUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(): UseCaseResult<FishingInsightsEntity> {
        return try {
            val result = catchRepository.getFishingInsights()
            result.fold(
                onSuccess = { data -> UseCaseResult.Success(data) },
                onFailure = { error -> 
                    UseCaseResult.Error(
                        error.message ?: "Failed to load fishing insights", 
                        error, 
                        "GetFishingInsightsUseCase"
                    )
                }
            )
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Unknown error", e, "GetFishingInsightsUseCase")
        }
    }
}
