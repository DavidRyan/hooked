package domain.usecase

import data.repo.CatchRepository
import data.model.CatchSubmissionResult
import domain.model.SubmitCatchRequest

class SubmitCatchUseCase(private val catchRepository: CatchRepository) {
    suspend operator fun invoke(request: SubmitCatchRequest): SubmitCatchUseCaseResult {
        return when(val result = catchRepository.submitCatch(request)) {
            is CatchSubmissionResult.Success -> {
                SubmitCatchUseCaseResult.Success(result.catchId)
            }
            is CatchSubmissionResult.Error -> {
                SubmitCatchUseCaseResult.Error(result.message)
            }
            CatchSubmissionResult.Loading -> {
                throw Exception("Loading state is not handled in this use case")
            }
        }
    }
}

sealed class SubmitCatchUseCaseResult {
    data class Success(val catchId: Long) : SubmitCatchUseCaseResult()
    data class Error(val message: String) : SubmitCatchUseCaseResult()
}