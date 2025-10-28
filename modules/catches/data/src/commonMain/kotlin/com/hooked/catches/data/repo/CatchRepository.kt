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
import com.hooked.catches.domain.entities.StatsEntity
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
            // Always try API first
            when (val result = catchApiService.getCatches()) {
                is NetworkResult.Success -> {
                    Logger.info("CatchRepository", "Fetched ${result.data.size} catches from API, caching locally")
                    localDataSource.insertCatches(result.data)
                    Result.success(result.data.map { it.toEntity() })
                }
                is NetworkResult.Error -> {
                    // Fallback to local database
                    val localCatches = localDataSource.getAllCatches().first()
                    
                    if (localCatches.isNotEmpty()) {
                        Logger.info("CatchRepository", "Returning ${localCatches.size} catches from local database")
                        Result.success(localCatches.map { it.toDomainEntity() })
                    } else {
                        // No local data either
                        Logger.error("CatchRepository", "No local cache available")
                        Result.failure(result.error)
                    }
                }
                NetworkResult.Loading -> Result.failure(Exception("Loading state not handled"))
            }
        } catch (e: Exception) {
            Logger.error("CatchRepository", "Error getting catches: ${e.message}", e)
            try {
                // Try local as last resort after exception
                val localCatches = localDataSource.getAllCatches().first()
                if (localCatches.isNotEmpty()) {
                    Logger.info("CatchRepository", "Exception caught but returning ${localCatches.size} catches from local database")
                    return Result.success(localCatches.map { it.toDomainEntity() })
                }
            } catch (dbException: Exception) {
                Logger.error("CatchRepository", "Failed to fetch from local DB after API exception", dbException)
            }
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
                is NetworkResult.Error -> {
                    Logger.error("CatchRepository", "Error refreshing catches from API: ${result.error.message}")
                    // On refresh we want the latest data, so we fail if the API call fails
                    Result.failure(result.error)
                }
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
    
    override suspend fun deleteCatch(catchId: String): Result<Unit> {
        return try {
            when (val result = catchApiService.deleteCatch(catchId)) {
                is NetworkResult.Success -> {
                    Logger.info("CatchRepository", "Successfully deleted catch: $catchId")
                    localDataSource.deleteCatch(catchId)
                    Result.success(Unit)
                }
                is NetworkResult.Error -> {
                    Logger.error("CatchRepository", "Failed to delete catch: ${result.error.message}", result.error)
                    Result.failure(result.error)
                }
                NetworkResult.Loading -> Result.failure(Exception("Loading state not handled"))
            }
        } catch (e: Exception) {
            Logger.error("CatchRepository", "Error deleting catch: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getCatchStats(): Result<StatsEntity> {
        return try {
            // For now, calculate stats client-side from all catches
            val catches = getCatches().getOrNull() ?: emptyList()

            val speciesBreakdown = catches
                .mapNotNull { it.name }
                .groupingBy { it }
                .eachCount()

            val stats = StatsEntity(
                totalCatches = catches.size,
                speciesBreakdown = speciesBreakdown,
                uniqueSpecies = speciesBreakdown.keys.size,
                uniqueLocations = catches.mapNotNull { it.location }.distinct().size,
                averageWeight = catches.mapNotNull { it.weight }.average().takeIf { !it.isNaN() },
                averageLength = catches.mapNotNull { it.length }.average().takeIf { !it.isNaN() },
                biggestCatch = catches.maxByOrNull { it.weight ?: 0.0 },
                mostRecentCatch = catches.maxByOrNull { it.dateCaught ?: "" }
            )

            Result.success(stats)
        } catch (e: Exception) {
            Logger.error("CatchRepository", "Error calculating catch stats: ${e.message}", e)
            Result.failure(e)
        }
    }
}