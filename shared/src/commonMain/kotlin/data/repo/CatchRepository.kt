package data.repo

import data.api.HookedApiService
import data.model.CatchDto
import data.model.SubmitCatchDto
import data.db.Database
import data.model.CatchResult
import data.model.CatchDetailsResult
import data.model.CatchSubmissionResult
import domain.model.NetworkResult
import domain.model.SubmitCatchRequest
import domain.repository.CatchRepositoy

class CatchRepository(
    private val hookedApiService: HookedApiService
) : CatchRepositoy {

    //private val queries = database.catchQueries

    suspend fun getCatches(): CatchResult {
        val result = hookedApiService.getCatches()
        return when(result) {
            is NetworkResult.Success -> CatchResult.Success(result.data)
            is NetworkResult.Error -> CatchResult.Error(result.error.message ?: "Unknown error")
            NetworkResult.Loading -> TODO()
        }
/*
        val cachedCatches = queries.selectAll().executeAsList().map {
            CatchDto(it.id, it.species, it.weight, it.length, it.photoUrl)
        }
        if (cachedCatches.isNotEmpty()) {
            return cachedCatches
        }
*/

    }
    
    suspend fun getCatchDetails(catchId: Long): CatchDetailsResult {
        val result = hookedApiService.getCatchDetails(catchId)
        return when(result) {
            is NetworkResult.Success -> CatchDetailsResult.Success(result.data)
            is NetworkResult.Error -> CatchDetailsResult.Error(result.error.message ?: "Unknown error")
            NetworkResult.Loading -> TODO()
        }
    }
    
    suspend fun submitCatch(request: SubmitCatchRequest): CatchSubmissionResult {
        val submitDto = SubmitCatchDto(
            species = request.species,
            weight = request.weight,
            length = request.length,
            latitude = request.latitude,
            longitude = request.longitude,
            photoBase64 = request.photoBase64, // Photo data WITH EXIF metadata preserved
            timestamp = request.timestamp
        )
        
        val result = hookedApiService.submitCatch(submitDto)
        return when(result) {
            is NetworkResult.Success -> CatchSubmissionResult.Success(result.data)
            is NetworkResult.Error -> CatchSubmissionResult.Error(result.error.message ?: "Unknown error")
            NetworkResult.Loading -> TODO()
        }
    }
}
