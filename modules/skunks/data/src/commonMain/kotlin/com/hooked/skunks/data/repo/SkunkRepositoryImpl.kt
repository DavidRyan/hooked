package com.hooked.skunks.data.repo

import com.hooked.skunks.data.api.SkunkApiService
import com.hooked.skunks.domain.entities.SubmitSkunkEntity
import com.hooked.skunks.domain.repositories.SkunkRepository

class SkunkRepositoryImpl(
    private val skunkApiService: SkunkApiService
) : SkunkRepository {

    override suspend fun submitSkunk(skunk: SubmitSkunkEntity): Result<Unit> {
        return try {
            skunkApiService.submitSkunk(
                fishedAt = skunk.fishedAt,
                latitude = skunk.latitude,
                longitude = skunk.longitude,
                notes = skunk.notes
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
