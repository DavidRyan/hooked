package com.hooked.submit.data.api

import com.hooked.submit.data.model.SubmitCatchDto
import com.hooked.core.config.NetworkConfig
import com.hooked.core.domain.NetworkResult
import com.hooked.core.logging.Logger
import com.hooked.core.logging.logRequest
import com.hooked.core.logging.logResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SubmitApiService(
    private val httpClient: HttpClient,
) {
    companion object {
        private const val TAG = "SubmitApiService"
    }
    
    suspend fun submitCatch(submitCatchDto: SubmitCatchDto): NetworkResult<String> {
        val endpoint = "/user_catches"
        Logger.logRequest(TAG, "POST", endpoint)
        try {
            val response = httpClient
                .post("${NetworkConfig.BASE_URL}$endpoint") {
                    contentType(ContentType.Application.Json)
                    setBody(submitCatchDto)
                }
                .body<Map<String, String>>()
            
            val catchId = response["id"] ?: throw Exception("No catch ID returned in response")
            Logger.logResponse(TAG, 201, "Created - ID: $catchId")
            return NetworkResult.Success(catchId)
        } catch (e: ClientRequestException) {
            return NetworkResult.Error(Exception(formatHttpError(e)), TAG)
        } catch (e: Exception) {
            return NetworkResult.Error(Exception("Failed to submit catch: ${e.message}", e), TAG)
        }
    }
    
    private suspend fun formatHttpError(e: ClientRequestException): String {
        val statusCode = e.response.status.value
        val statusText = e.response.status.description
        val body = try { e.response.bodyAsText() } catch (_: Exception) { "Unable to read response body" }
        return "[$statusCode $statusText] $body"
    }
}
