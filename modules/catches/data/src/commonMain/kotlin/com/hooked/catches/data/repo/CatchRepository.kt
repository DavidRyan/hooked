package com.hooked.catches.data.repo

import com.hooked.catches.data.api.CatchApiService
import com.hooked.catches.data.model.toEntity
import com.hooked.catches.data.model.toCatchDetailsEntity
import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.entities.CatchDetailsEntity
import com.hooked.catches.domain.repositories.CatchRepository as CatchRepositoryInterface
import com.hooked.core.domain.NetworkResult

class CatchRepositoryImpl(
    private val catchApiService: CatchApiService
) : CatchRepositoryInterface {
    
    override suspend fun getCatches(): Result<List<CatchEntity>> {
        return when(val result = catchApiService.getCatches()) {
            is NetworkResult.Success -> Result.success(result.data.map { it.toEntity() })
            is NetworkResult.Error -> Result.failure(result.error)
            NetworkResult.Loading -> Result.failure(Exception("Loading state not handled"))
        }
    }
    
    override suspend fun getCatchDetails(catchId: Long): Result<CatchDetailsEntity> {
        return when(val result = catchApiService.getCatchDetails(catchId)) {
            is NetworkResult.Success -> Result.success(result.data.toCatchDetailsEntity())
            is NetworkResult.Error -> Result.failure(result.error)
            NetworkResult.Loading -> Result.failure(Exception("Loading state not handled"))
        }
    }
}