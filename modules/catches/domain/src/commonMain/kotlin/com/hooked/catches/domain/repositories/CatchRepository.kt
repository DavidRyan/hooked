package com.hooked.catches.domain.repositories

import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.entities.CatchDetailsEntity
import com.hooked.core.domain.common.Result

/**
 * Repository interface for catch-related operations
 * Follows the Repository pattern from Clean Architecture
 */
interface CatchRepository {
    
    /**
     * Retrieves all catches
     * @return Result containing list of catch entities or error
     */
    suspend fun getCatches(): Result<List<CatchEntity>>
    
    /**
     * Retrieves details for a specific catch
     * @param catchId The ID of the catch to retrieve
     * @return Result containing catch details entity or error
     */
    suspend fun getCatchDetails(catchId: Long): Result<CatchDetailsEntity>
}