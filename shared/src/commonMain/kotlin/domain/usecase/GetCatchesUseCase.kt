package domain.usecase

import data.repo.CatchRepository
import domain.model.CatchEntity
import domain.model.toEntity


class GetCatchesUseCase(private val catchGridRepository: CatchRepository) {
    suspend operator fun invoke(): List<CatchEntity> {
        return catchGridRepository.getCatches().map {
            it.toEntity()
        }
    }
}
