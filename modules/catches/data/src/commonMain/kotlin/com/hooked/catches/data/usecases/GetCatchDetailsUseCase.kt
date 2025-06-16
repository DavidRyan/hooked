package com.hooked.catches.data.usecases

import com.hooked.catches.domain.entities.CatchDetailsEntity
import com.hooked.catches.data.model.toCatchDetailsEntity
import com.hooked.catches.data.repo.CatchRepository
import com.hooked.catches.data.model.CatchDetailsResult

class GetCatchDetailsUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(catchId: Long): GetCatchDetailsUseCaseResult {
        return when(val result = catchRepository.getCatchDetails(catchId)) {
            is CatchDetailsResult.Success -> {
                GetCatchDetailsUseCaseResult.Success(result.catch.toCatchDetailsEntity())
            }
            is CatchDetailsResult.Error -> {
                GetCatchDetailsUseCaseResult.Error(result.message)
            }
            CatchDetailsResult.Loading -> {
                throw Exception("Loading state is not handled in this use case")
            }
        }
    }
}

sealed class GetCatchDetailsUseCaseResult {
    data class Success(val catchDetails: CatchDetailsEntity) : GetCatchDetailsUseCaseResult()
    data class Error(val message: String) : GetCatchDetailsUseCaseResult()
}
