package com.hooked.catches.domain.usecases

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.core.domain.common.Result

/**
 * Use case for retrieving all catches
 */
class GetCatchesUseCase(
    private val repository: CatchRepository
) {
    suspend operator fun invoke(): Result<List<CatchEntity>> {
        return repository.getCatches()
    }
}