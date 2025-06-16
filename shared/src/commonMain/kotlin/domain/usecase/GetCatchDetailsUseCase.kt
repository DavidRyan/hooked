package domain.usecase

import data.repo.CatchRepository
import data.model.CatchDetailsResult
import domain.model.CatchDetailsEntity
import domain.model.toCatchDetailsEntity

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
