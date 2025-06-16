package domain.usecase

import domain.repository.CatchRepository
import domain.common.Result
import domain.model.SubmitCatchRequest

class SubmitCatchUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(request: SubmitCatchRequest): SubmitCatchUseCaseResult {
        return when(val result = catchRepository.submitCatch(request)) {
            is Result.Success -> {
                SubmitCatchUseCaseResult.Success(result.data)
            }
            is Result.Error -> {
                SubmitCatchUseCaseResult.Error(result.message)
            }
            Result.Loading -> {
                throw Exception("Loading state is not handled in this use case")
            }
        }
    }
}

sealed class SubmitCatchUseCaseResult {
    data class Success(val catchId: Long) : SubmitCatchUseCaseResult()
    data class Error(val message: String) : SubmitCatchUseCaseResult()
}