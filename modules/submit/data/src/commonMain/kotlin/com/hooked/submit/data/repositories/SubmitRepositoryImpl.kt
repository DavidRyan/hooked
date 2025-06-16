package com.hooked.submit.data.repositories

import com.hooked.submit.domain.entities.SubmitCatchRequest
import com.hooked.submit.domain.repositories.SubmitRepository
import com.hooked.core.domain.common.Result
import com.hooked.shared.network.HookedApiService
import com.hooked.shared.network.model.SubmitCatchDto

/**
 * Implementation of SubmitRepository using network data source
 */
class SubmitRepositoryImpl(
    private val apiService: HookedApiService
) : SubmitRepository {

    override suspend fun submitCatch(request: SubmitCatchRequest): Result<Long> {
        val submitDto = SubmitCatchDto(
            species = request.species,
            weight = request.weight,
            length = request.length,
            latitude = request.latitude,
            longitude = request.longitude,
            photoBase64 = request.photoBase64, // Photo data WITH EXIF metadata preserved
            timestamp = request.timestamp
        )
        
        return when (val result = apiService.submitCatch(submitDto)) {
            is Result.Success -> Result.Success(result.data)
            is Result.Error -> Result.Error(result.exception, result.message)
            Result.Loading -> Result.Loading
        }
    }
}