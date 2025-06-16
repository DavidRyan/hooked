package com.hooked.submit.domain.usecases

import com.hooked.submit.domain.entities.SubmitCatchRequest
import com.hooked.submit.domain.repositories.SubmitRepository
import com.hooked.core.domain.common.Result

/**
 * Use case for submitting a catch
 */
class SubmitCatchUseCase(
    private val repository: SubmitRepository
) {
    suspend operator fun invoke(request: SubmitCatchRequest): Result<Long> {
        return repository.submitCatch(request)
    }
}