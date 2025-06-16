package com.hooked.submit.domain.repositories

import com.hooked.submit.domain.entities.SubmitCatchRequest
import com.hooked.core.domain.common.Result

/**
 * Repository interface for catch submission operations
 */
interface SubmitRepository {
    
    /**
     * Submits a new catch
     * @param request The catch submission request
     * @return Result containing the ID of the created catch or error
     */
    suspend fun submitCatch(request: SubmitCatchRequest): Result<Long>
}