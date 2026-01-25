package com.hooked.submit.data.repo

import com.hooked.submit.data.api.SubmitApiService
import com.hooked.submit.data.model.SubmitCatchDto
import com.hooked.submit.domain.entities.SubmitCatchEntity
import com.hooked.submit.domain.repositories.SubmitRepository
import com.hooked.core.domain.NetworkResult

class SubmitRepositoryImpl(
    private val submitApiService: SubmitApiService
) : SubmitRepository {
    
    override suspend fun submitCatch(catchEntity: SubmitCatchEntity): Result<String> {
        val submitDto = SubmitCatchDto(
            species = catchEntity.species,
            weight = 0.0, // TODO: Add weight field to SubmitCatchEntity
            length = 0.0, // TODO: Add length field to SubmitCatchEntity  
            latitude = catchEntity.latitude,
            longitude = catchEntity.longitude,
            photoBytes = catchEntity.photoBytes,
            timestamp = System.currentTimeMillis() // Use current timestamp
        )
        
        return when(val result = submitApiService.submitCatch(submitDto)) {
            is NetworkResult.Success -> Result.success(result.data)
            is NetworkResult.Error -> Result.failure(result.error)
            NetworkResult.Loading -> Result.failure(Exception("Loading state not handled"))
        }
    }
}
