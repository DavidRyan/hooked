package com.hooked.shared.network

import com.hooked.core.domain.common.Result
import com.hooked.shared.network.model.CatchDto
import com.hooked.shared.network.model.SubmitCatchDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Defines the network API operations. Stub for now.
 */
class HookedApiService(
    private val httpClient: HttpClient,
) {
    suspend fun getCatches(): Result<List<CatchDto>> {
        val remoteCatches = httpClient
            .get("http://10.0.2.2:8080/catches")
            .body<List<CatchDto>>()
        return Result.Success(remoteCatches)
    }
    
    suspend fun getCatchDetails(catchId: Long): Result<CatchDto> {
        try {
            val catchDetails = httpClient
                .get("http://10.0.2.2:8080/catches/$catchId")
                .body<CatchDto>()
            return Result.Success(catchDetails)
        } catch (e: Exception) {
            return Result.Error(e, "Failed to fetch catch details")
        }
    }
    
    suspend fun submitCatch(submitCatchDto: SubmitCatchDto): Result<Long> {
        try {
            val response = httpClient
                .post("http://10.0.2.2:8080/catches") {
                    contentType(ContentType.Application.Json)
                    setBody(submitCatchDto)
                }
                .body<Map<String, Long>>()
            
            val catchId = response["id"] ?: throw Exception("No catch ID returned")
            return Result.Success(catchId)
        } catch (e: Exception) {
            return Result.Error(e, "Failed to submit catch")
        }
    }
}
