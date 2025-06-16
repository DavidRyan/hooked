package com.hooked.catches.data.repositories

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.entities.CatchDetailsEntity
import com.hooked.catches.domain.repositories.CatchRepository
import com.hooked.catches.data.mappers.toEntity
import com.hooked.catches.data.mappers.toCatchDetailsEntity
import com.hooked.core.domain.common.Result
import com.hooked.shared.network.HookedApiService

/**
 * Implementation of CatchRepository using network data source
 */
class CatchRepositoryImpl(
    private val apiService: HookedApiService
) : CatchRepository {

    override suspend fun getCatches(): Result<List<CatchEntity>> {
        return when (val result = apiService.getCatches()) {
            is Result.Success -> Result.Success(result.data.map { it.toEntity() })
            is Result.Error -> Result.Error(result.exception, result.message)
            Result.Loading -> Result.Loading
        }
    }
    
    override suspend fun getCatchDetails(catchId: Long): Result<CatchDetailsEntity> {
        return when (val result = apiService.getCatchDetails(catchId)) {
            is Result.Success -> Result.Success(result.data.toCatchDetailsEntity())
            is Result.Error -> Result.Error(result.exception, result.message)
            Result.Loading -> Result.Loading
        }
    }
}