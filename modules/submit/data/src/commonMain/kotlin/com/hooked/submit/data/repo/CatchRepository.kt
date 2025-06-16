package com.hooked.submit.data.repo

import com.hooked.submit.data.api.SubmitApiService
import com.hooked.submit.data.model.SubmitCatchDto
import com.hooked.submit.data.model.CatchSubmissionResult
import com.hooked.core.domain.NetworkResult
import com.hooked.submit.data.model.SubmitCatchRequest

class CatchRepository(
    private val submitApiService: SubmitApiService
) {
    suspend fun submitCatch(request: SubmitCatchRequest): CatchSubmissionResult {
        val submitDto = SubmitCatchDto(
            species = request.species,
            weight = request.weight,
            length = request.length,
            latitude = request.latitude,
            longitude = request.longitude,
            photoBase64 = request.photoBase64,
            timestamp = request.timestamp
        )
        
        val result = submitApiService.submitCatch(submitDto)
        return when(result) {
            is NetworkResult.Success -> CatchSubmissionResult.Success(result.data)
            is NetworkResult.Error -> CatchSubmissionResult.Error(result.error.message ?: "Unknown error")
            NetworkResult.Loading -> TODO()
        }
    }
}