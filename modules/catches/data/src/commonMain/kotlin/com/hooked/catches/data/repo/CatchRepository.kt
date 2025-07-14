package com.hooked.catches.data.repo

import com.hooked.catches.data.api.CatchApiService
import com.hooked.catches.data.database.CatchLocalDataSource
import com.hooked.catches.data.database.toCatchDetailsEntity
import com.hooked.catches.data.database.toDomainEntity
import com.hooked.catches.data.model.toEntity
import com.hooked.catches.data.model.toCatchDetailsEntity
import com.hooked.catches.data.model.SubmitCatchDto
import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.entities.CatchDetailsEntity
import com.hooked.catches.domain.entities.SubmitCatchEntity
import com.hooked.catches.domain.repositories.CatchRepository as CatchRepositoryInterface
import com.hooked.core.domain.NetworkResult
import com.hooked.core.logging.Logger
import kotlinx.coroutines.flow.first

class CatchRepositoryImpl(
    private val catchApiService: CatchApiService,
    private val localDataSource: CatchLocalDataSource
) : CatchRepositoryInterface {
    
    override suspend fun getCatches(): Result<List<CatchEntity>> {
        return try {
            // First try to get from local database
            val localCatches = localDataSource.getAllCatches().first()
            
            if (localCatches.isNotEmpty()) {
                Logger.info("CatchRepository", "Returning ${localCatches.size} catches from local database")
                Result.success(localCatches.map { it.toDomainEntity() })
            } else {
                // If no local data, fetch from API and cache
                when (val result = catchApiService.getCatches()) {
                    is NetworkResult.Success -> {
                        Logger.info("CatchRepository", "Fetched ${result.data.size} catches from API, caching locally")
                        localDataSource.insertCatches(result.data)
                        Result.success(result.data.map { it.toEntity() })
                    }
                    is NetworkResult.Error -> Result.failure(result.error)
                    NetworkResult.Loading -> Result.failure(Exception("Loading state not handled"))
                }
            }
        } catch (e: Exception) {
            Logger.error("CatchRepository", "Error getting catches: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getCatchDetails(catchId: String): Result<CatchDetailsEntity> {
        return try {
            // First try to get from local database
            val localCatch = localDataSource.getCatchById(catchId)
            
            if (localCatch != null) {
                Logger.info("CatchRepository", "Returning catch details for id $catchId from local database")
                Result.success(localCatch.toCatchDetailsEntity())
            } else {
                // If not in local database, fetch from API
                when (val result = catchApiService.getCatchDetails(catchId)) {
                    is NetworkResult.Success -> {
                        Logger.info("CatchRepository", "Fetched catch details for id $catchId from API")
                        // Cache the result
                        localDataSource.insertCatch(result.data)
                        Result.success(result.data.toCatchDetailsEntity())
                    }
                    is NetworkResult.Error -> Result.failure(result.error)
                    NetworkResult.Loading -> Result.failure(Exception("Loading state not handled"))
                }
            }
        } catch (e: Exception) {
            Logger.error("CatchRepository", "Error getting catch details for id $catchId: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun refreshCatches(): Result<List<CatchEntity>> {
        return try {
            when (val result = catchApiService.getCatches()) {
                is NetworkResult.Success -> {
                    Logger.info("CatchRepository", "Refreshing catches from API")
                    // Clear local cache and insert fresh data
                    localDataSource.deleteAllCatches()
                    localDataSource.insertCatches(result.data)
                    Result.success(result.data.map { it.toEntity() })
                }
                is NetworkResult.Error -> Result.failure(result.error)
                NetworkResult.Loading -> Result.failure(Exception("Loading state not handled"))
            }
        } catch (e: Exception) {
            Logger.error("CatchRepository", "Error refreshing catches: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun submitCatch(catchEntity: SubmitCatchEntity): Result<String> {
        val submitDto = SubmitCatchDto(
            species = catchEntity.species,
            location = catchEntity.location ?: "Unknown",
            latitude = catchEntity.latitude,
            longitude = catchEntity.longitude,
            caughtAt = catchEntity.caughtAt ?: "2024-01-01T00:00:00Z",
            notes = catchEntity.notes
        )
        
        return try {
            when(val result = catchApiService.submitCatch(submitDto, catchEntity.imageBytes)) {
                is NetworkResult.Success -> {
                    Logger.info("CatchRepository", "Successfully submitted catch, received ID: ${result.data}")
                    Result.success(result.data)
                }
                is NetworkResult.Error -> {
                    Logger.error("CatchRepository", "Failed to submit catch: ${result.error.message}", result.error)
                    Result.failure(result.error)
                }
                NetworkResult.Loading -> Result.failure(Exception("Loading state not handled"))
            }
        } catch (e: Exception) {
            Logger.error("CatchRepository", "Error submitting catch: ${e.message}", e)
            Result.failure(e)
        }
    }
}