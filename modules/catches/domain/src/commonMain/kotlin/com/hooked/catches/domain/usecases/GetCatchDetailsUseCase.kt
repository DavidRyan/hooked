package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.entities.CatchDetailsEntity
import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.core.domain.common.Result

/**
 * Use case for retrieving catch details
 */
class GetCatchDetailsUseCase(
    private val repository: CatchRepository
) {
    suspend operator fun invoke(catchId: Long): Result<CatchDetailsEntity> {
        return repository.getCatchDetails(catchId)
    }
}