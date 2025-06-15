package domain.usecase

import data.model.CatchResult
import data.repo.CatchRepository
import domain.model.CatchEntity
import domain.model.toEntity


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

// make this a sealed class
sealed class GetCatchesUseCaseResult {
    data class Success(val catches: List<CatchEntity>) : GetCatchesUseCaseResult()
    data class Error(val message: String) : GetCatchesUseCaseResult()
}