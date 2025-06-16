package data.repo

import data.api.HookedApiService
import data.model.CatchDto
import data.model.SubmitCatchDto
import data.db.Database
import data.model.CatchResult
import data.model.CatchDetailsResult
import data.model.CatchSubmissionResult
import domain.model.NetworkResult
import domain.model.toEntity
import domain.model.toCatchDetailsEntity
import domain.common.Result
import domain.model.SubmitCatchRequest
import domain.repository.CatchRepository

class CatchRepository(
    private val hookedApiService: HookedApiService
) : CatchRepository {

    //private val queries = database.catchQueries

    override suspend fun getCatches(): domain.common.Result<List<domain.model.CatchEntity>> {
        val result = hookedApiService.getCatches()
        return when(result) {
            is NetworkResult.Success -> Result.Success(result.data.map { it.toEntity() })
            is NetworkResult.Error -> Result.Error(result.error, result.error.message ?: "Unknown error")
            NetworkResult.Loading -> Result.Loading
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
    
    override suspend fun getCatchDetails(catchId: Long): domain.common.Result<domain.model.CatchDetailsEntity> {
        val result = hookedApiService.getCatchDetails(catchId)
        return when(result) {
            is NetworkResult.Success -> Result.Success(result.data.toCatchDetailsEntity())
            is NetworkResult.Error -> Result.Error(result.error, result.error.message ?: "Unknown error")
            NetworkResult.Loading -> Result.Loading
        }
    }
    
    override suspend fun submitCatch(request: SubmitCatchRequest): domain.common.Result<Long> {
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
            is NetworkResult.Success -> Result.Success(result.data)
            is NetworkResult.Error -> Result.Error(result.error, result.error.message ?: "Unknown error")
            NetworkResult.Loading -> Result.Loading
        }
    }
}
