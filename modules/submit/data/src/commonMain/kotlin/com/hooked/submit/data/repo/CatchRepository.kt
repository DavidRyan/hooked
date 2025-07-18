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
            weight = catchEntity.weight,
            length = catchEntity.length,
            latitude = catchEntity.latitude,
            longitude = catchEntity.longitude,
            photoBase64 = catchEntity.photoBase64,
            timestamp = catchEntity.timestamp
        )
        
        return when(val result = submitApiService.submitCatch(submitDto)) {
            is NetworkResult.Success -> Result.success(result.data)
            is NetworkResult.Error -> Result.failure(result.error)
            NetworkResult.Loading -> Result.failure(Exception("Loading state not handled"))
        }
    }
}