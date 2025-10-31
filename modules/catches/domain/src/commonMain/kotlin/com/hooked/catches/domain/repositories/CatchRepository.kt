package com.hooked.catches.domain.repositories

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.entities.CatchDetailsEntity
import com.hooked.catches.domain.entities.FishingInsightsEntity
import com.hooked.catches.domain.entities.StatsEntity
import com.hooked.catches.domain.entities.SubmitCatchEntity

interface CatchRepository {
    suspend fun getCatches(): Result<List<CatchEntity>>
    suspend fun getCatchDetails(catchId: String): Result<CatchDetailsEntity>
    suspend fun submitCatch(catchEntity: SubmitCatchEntity): Result<String>
    suspend fun deleteCatch(catchId: String): Result<Unit>
    suspend fun getCatchStats(): Result<StatsEntity>
    suspend fun getFishingInsights(): Result<FishingInsightsEntity>
}