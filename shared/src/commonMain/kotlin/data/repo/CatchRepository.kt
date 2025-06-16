package data.repo

import data.api.HookedApiService
import data.model.CatchDto
import data.db.Database
import data.model.CatchResult
import data.model.CatchDetailsResult
import domain.model.NetworkResult
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
}
