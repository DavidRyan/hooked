package usecase

import com.hooked.data.repository.CatchGridRepository
import com.hooked.domain.model.CatchModel

class GetCatchesUseCase(private val catchGridRepository: CatchGridRepository) {
    suspend operator fun invoke(): List<CatchModel> {
        return catchGridRepository.getCatches()
    }
}
