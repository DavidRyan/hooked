package com.hooked.catches.data.usecases

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.data.model.toEntity
import com.hooked.catches.data.repo.CatchRepository
import com.hooked.catches.data.model.CatchResult


class GetCatchesUseCase(private val catchGridRepository: CatchRepository) {
    suspend operator fun invoke(): GetCatchesUseCaseResult {
        return when(val result = catchGridRepository.getCatches()) {
            is CatchResult.Success -> {
                GetCatchesUseCaseResult.Success(result.catches.map { it.toEntity() })
            }

            is CatchResult.Error -> {
                GetCatchesUseCaseResult.Error(result.message)
            }

            CatchResult.Loading -> {
                throw Exception("Loading state is not handled in this use case")
            }
        }
    }
}

sealed class GetCatchesUseCaseResult {
    data class Success(val catches: List<CatchEntity>) : GetCatchesUseCaseResult()
    data class Error(val message: String) : GetCatchesUseCaseResult()
}