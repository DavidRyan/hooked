package usecase

import com.hooked.data.repository.CatchDetailsRepository
import com.hooked.domain.model.CatchDetailsModel

class GetCatchDetailsUseCase(private val catchDetailsRepository: CatchDetailsRepository) {
    suspend operator fun invoke(catchId: Long): CatchDetailsModel {
        return catchDetailsRepository.getCatchDetails(catchId)
    }
}
