package com.hooked.catches.data.repo

import com.hooked.catches.data.api.CatchApiService
import com.hooked.catches.data.model.CatchResult
import com.hooked.catches.data.model.CatchDetailsResult
import com.hooked.core.domain.NetworkResult

class CatchRepository(
    private val catchApiService: CatchApiService
) {
    suspend fun getCatches(): CatchResult {
        val result = catchApiService.getCatches()
        return when(result) {
            is NetworkResult.Success -> CatchResult.Success(result.data)
            is NetworkResult.Error -> CatchResult.Error(result.error.message ?: "Unknown error")
            NetworkResult.Loading -> TODO()
        }
    }
    
    suspend fun getCatchDetails(catchId: Long): CatchDetailsResult {
        val result = catchApiService.getCatchDetails(catchId)
        return when(result) {
            is NetworkResult.Success -> CatchDetailsResult.Success(result.data)
            is NetworkResult.Error -> CatchDetailsResult.Error(result.error.message ?: "Unknown error")
            NetworkResult.Loading -> TODO()
        }
    }
}