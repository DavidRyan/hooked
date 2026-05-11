package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.entities.RibbonInsightEntity
import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.core.domain.UseCaseResult

class GetRibbonInsightUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(): UseCaseResult<RibbonInsightEntity> {
        return try {
            catchRepository.getRibbonInsight().fold(
                onSuccess = { data -> UseCaseResult.Success(data) },
                onFailure = { error ->
                    UseCaseResult.Error(
                        error.message ?: "Failed to load ribbon insight",
                        error,
                        "GetRibbonInsightUseCase"
                    )
                }
            )
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Unknown error", e, "GetRibbonInsightUseCase")
        }
    }
}
