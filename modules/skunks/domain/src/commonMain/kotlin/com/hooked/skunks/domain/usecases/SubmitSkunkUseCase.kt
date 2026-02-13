package com.hooked.skunks.domain.usecases

import com.hooked.skunks.domain.entities.SubmitSkunkEntity
import com.hooked.skunks.domain.repositories.SkunkRepository

class SubmitSkunkUseCase(
    private val skunkRepository: SkunkRepository
) {
    suspend operator fun invoke(skunk: SubmitSkunkEntity): Result<Unit> {
        return skunkRepository.submitSkunk(skunk)
    }
}
