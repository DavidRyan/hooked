package usecase

import grid.model.CatchModel


class GetCatchesUseCase(/*private val catchGridRepository: CatchGridRepository*/) {
    suspend operator fun invoke(): List<CatchModel> {
        TODO()
        //return catchGridRepository.getCatches()
    }
}
