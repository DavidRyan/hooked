package domain.usecase

import domain.repository.CatchRepository
import domain.common.Result
import domain.model.CatchEntity


class GetCatchesUseCase(private val catchGridRepository: CatchRepository) {
    suspend operator fun invoke(): GetCatchesUseCaseResult {
        return when(val result = catchGridRepository.getCatches()) {
            is Result.Success -> {
                GetCatchesUseCaseResult.Success(result.data)
            }

            is Result.Error -> {
                GetCatchesUseCaseResult.Error(result.message)
            }

            Result.Loading -> {
                throw Exception("Loading state is not handled in this use case")
            }
        }
    }
}

// make this a sealed class
sealed class GetCatchesUseCaseResult {
    data class Success(val catches: List<CatchEntity>) : GetCatchesUseCaseResult()
    data class Error(val message: String) : GetCatchesUseCaseResult()
}