package data.api

import data.model.CatchDto
import data.model.SubmitCatchDto
import domain.model.NetworkResult
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
    suspend fun getCatches(): NetworkResult<List<CatchDto>> {
        val remoteCatches = httpClient
            .get("http://10.0.2.2:8080/catches")
            .body<List<CatchDto>>()
        return NetworkResult.Success(remoteCatches)
    }
    
    suspend fun getCatchDetails(catchId: Long): NetworkResult<CatchDto> {
        try {
            val catchDetails = httpClient
                .get("http://10.0.2.2:8080/catches/$catchId")
                .body<CatchDto>()
            return NetworkResult.Success(catchDetails)
        } catch (e: Exception) {
            return NetworkResult.Error(Exception("Failed to fetch catch details: ${e.message}"))
        }
    }
    
    suspend fun submitCatch(submitCatchDto: SubmitCatchDto): NetworkResult<Long> {
        try {
            val response = httpClient
                .post("http://10.0.2.2:8080/catches") {
                    contentType(ContentType.Application.Json)
                    setBody(submitCatchDto)
                }
                .body<Map<String, Long>>()
            
            val catchId = response["id"] ?: throw Exception("No catch ID returned")
            return NetworkResult.Success(catchId)
        } catch (e: Exception) {
            return NetworkResult.Error(Exception("Failed to submit catch: ${e.message}"))
        }
    }
}
