package com.hooked.submit.domain.repositories

import com.hooked.submit.domain.entities.SubmitCatchEntity

interface SubmitRepository {
    suspend fun submitCatch(catchEntity: SubmitCatchEntity): Result<String>
}