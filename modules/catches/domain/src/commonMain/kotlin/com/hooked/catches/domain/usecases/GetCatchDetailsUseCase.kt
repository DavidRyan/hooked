package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.entities.CatchDetailsEntity
import com.hooked.catches.domain.repositories.CatchRepository

class GetCatchDetailsUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(catchId: Long): GetCatchDetailsUseCaseResult {
        return try {
            val result = catchRepository.getCatchDetails(catchId)
            if (result.isSuccess) {
                result.getOrNull()?.let { catchDetails ->
                    GetCatchDetailsUseCaseResult.Success(catchDetails)
                } ?: GetCatchDetailsUseCaseResult.Error("Catch details not found")
            } else {
                GetCatchDetailsUseCaseResult.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            GetCatchDetailsUseCaseResult.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class GetCatchDetailsUseCaseResult {
    data class Success(val catchDetails: CatchDetailsEntity) : GetCatchDetailsUseCaseResult()
    data class Error(val message: String) : GetCatchDetailsUseCaseResult()
}