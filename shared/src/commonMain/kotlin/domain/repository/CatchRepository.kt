package domain.repository

import domain.common.Result
import domain.model.CatchDetailsEntity
import domain.model.CatchEntity
import domain.model.SubmitCatchRequest

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
    
    /**
     * Submits a new catch
     * @param request The catch submission request
     * @return Result containing the ID of the created catch or error
     */
    suspend fun submitCatch(request: SubmitCatchRequest): Result<Long>
}