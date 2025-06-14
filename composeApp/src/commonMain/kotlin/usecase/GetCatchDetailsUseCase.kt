package usecase

import details.model.CatchDetailsModel


class GetCatchDetailsUseCase(/*private val catchDetailsRepository: CatchDetailsRepository*/) {
    suspend operator fun invoke(catchId: Long): CatchDetailsModel {
        TODO()
    }
}
