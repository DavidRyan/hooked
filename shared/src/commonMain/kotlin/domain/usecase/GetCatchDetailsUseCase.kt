package domain.usecase

import domain.repository.CatchRepository
import domain.common.Result
import domain.model.CatchDetailsEntity

class GetCatchDetailsUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(catchId: Long): GetCatchDetailsUseCaseResult {
        return when(val result = catchRepository.getCatchDetails(catchId)) {
            is Result.Success -> {
                GetCatchDetailsUseCaseResult.Success(result.data)
            }
            is Result.Error -> {
                GetCatchDetailsUseCaseResult.Error(result.message)
            }
            Result.Loading -> {
                throw Exception("Loading state is not handled in this use case")
            }
        }
    }
}

sealed class GetCatchDetailsUseCaseResult {
    data class Success(val catchDetails: CatchDetailsEntity) : GetCatchDetailsUseCaseResult()
    data class Error(val message: String) : GetCatchDetailsUseCaseResult()
}
