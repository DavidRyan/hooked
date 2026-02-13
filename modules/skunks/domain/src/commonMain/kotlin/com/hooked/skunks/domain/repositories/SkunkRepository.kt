package com.hooked.skunks.domain.repositories

import com.hooked.skunks.domain.entities.SubmitSkunkEntity

interface SkunkRepository {
    suspend fun submitSkunk(skunk: SubmitSkunkEntity): Result<Unit>
}
