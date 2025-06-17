package com.hooked.catches.domain.repositories

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.entities.CatchDetailsEntity

interface CatchRepository {
    suspend fun getCatches(): Result<List<CatchEntity>>
    suspend fun getCatchDetails(catchId: Long): Result<CatchDetailsEntity>
}