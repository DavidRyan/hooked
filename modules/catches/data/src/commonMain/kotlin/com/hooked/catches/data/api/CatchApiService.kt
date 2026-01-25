package com.hooked.catches.data.api

import com.hooked.catches.data.model.CatchDto
import com.hooked.catches.data.model.SubmitCatchDto
import com.hooked.core.config.NetworkConfig
import com.hooked.core.domain.NetworkResult
import com.hooked.core.logging.Logger
import com.hooked.core.logging.logRequest
import com.hooked.core.logging.logResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class CatchApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String = NetworkConfig.BASE_URL
) {
    companion object {
        private const val TAG = "CatchApiService"
    }
    
    suspend fun getCatches(): NetworkResult<List<CatchDto>> {
        val endpoint = "/user_catches"
        Logger.logRequest(TAG, "GET", endpoint)
        return try {
            val response = httpClient
                .get("$baseUrl$endpoint")
                .body<Map<String, List<CatchDto>>>()
            
            val catches = response["user_catches"] ?: emptyList()
            Logger.logResponse(TAG, 200, "OK - $catches catches")
            NetworkResult.Success(catches)
        } catch (e: ClientRequestException) {
            NetworkResult.Error(Exception(formatHttpError(e)), TAG)
        } catch (e: Exception) {
            NetworkResult.Error(Exception("Failed to fetch catches: ${e.message}", e), TAG)
        }
    }
    
    suspend fun getCatchDetails(catchId: String): NetworkResult<CatchDto> {
        val endpoint = "/user_catches/$catchId"
        Logger.logRequest(TAG, "GET", endpoint)
        return try {
            val response = httpClient
                .get("$baseUrl$endpoint")
                .body<Map<String, CatchDto>>()
            
            val catchDetails = response["user_catch"] 
                ?: return NetworkResult.Error(Exception("Catch not found in response"), TAG)
            
            Logger.logResponse(TAG, 200, "OK")
            NetworkResult.Success(catchDetails)
        } catch (e: ClientRequestException) {
            NetworkResult.Error(Exception(formatHttpError(e)), TAG)
        } catch (e: Exception) {
            NetworkResult.Error(Exception("Failed to fetch catch details for ID $catchId: ${e.message}", e), TAG)
        }
    }
    
    suspend fun submitCatch(submitCatchDto: SubmitCatchDto): NetworkResult<String> {
        val endpoint = "/user_catches"
        Logger.logRequest(TAG, "POST", endpoint)
        return try {
            val response = httpClient.post("$baseUrl$endpoint") {
                setBody(MultiPartFormDataContent(
                    formData {
                        submitCatchDto.species?.let { append("user_catch[species]", it) }
                        submitCatchDto.location?.let { append("user_catch[location]", it) }
                        submitCatchDto.caughtAt?.let { append("user_catch[caught_at]", it) }
                        submitCatchDto.latitude?.let { append("user_catch[latitude]", it.toString()) }
                        submitCatchDto.longitude?.let { append("user_catch[longitude]", it.toString()) }
                        submitCatchDto.notes?.let { append("user_catch[notes]", it) }
                        
                        submitCatchDto.imageBytes?.let { imageBytes ->
                            append(
                                "image",
                                imageBytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, "image/jpeg")
                                    append(HttpHeaders.ContentDisposition, "filename=catch.jpg")
                                }
                            )
                        }
                    }
                ))
            }.body<Map<String, CatchDto>>()
            
            val userCatch = response["user_catch"] 
                ?: return NetworkResult.Error(Exception("No catch returned in response"), TAG)
            
            Logger.logResponse(TAG, 201, "Created - ID: ${userCatch.id}")
            NetworkResult.Success(userCatch.id)
        } catch (e: ClientRequestException) {
            NetworkResult.Error(Exception(formatHttpError(e)), TAG)
        } catch (e: Exception) {
            NetworkResult.Error(Exception("Failed to submit catch: ${e.message}", e), TAG)
        }
    }
    
    suspend fun deleteCatch(catchId: String): NetworkResult<Unit> {
        val endpoint = "/user_catches/$catchId"
        Logger.logRequest(TAG, "DELETE", endpoint)
        return try {
            httpClient.delete("$baseUrl$endpoint")
            Logger.logResponse(TAG, 204, "Deleted")
            NetworkResult.Success(Unit)
        } catch (e: ClientRequestException) {
            NetworkResult.Error(Exception(formatHttpError(e)), TAG)
        } catch (e: Exception) {
            NetworkResult.Error(Exception("Failed to delete catch: ${e.message}", e), TAG)
        }
    }
    
    suspend fun getAiInsights(): NetworkResult<String> {
        val endpoint = "/ai/insights"
        Logger.logRequest(TAG, "GET", endpoint)
        return try {
            val response = httpClient
                .get("$baseUrl$endpoint")
                .body<Map<String, String>>()
            
            val insights = response["insights"] 
                ?: return NetworkResult.Error(Exception("No insights returned in response"), TAG)
            
            Logger.logResponse(TAG, 200, "OK")
            NetworkResult.Success(insights)
        } catch (e: ClientRequestException) {
            NetworkResult.Error(Exception(formatHttpError(e)), TAG)
        } catch (e: Exception) {
            NetworkResult.Error(Exception("Failed to fetch AI insights: ${e.message}", e), TAG)
        }
    }
    
    private suspend fun formatHttpError(e: ClientRequestException): String {
        val statusCode = e.response.status.value
        val statusText = e.response.status.description
        val body = try { 
            e.response.bodyAsText() 
        } catch (bodyException: Exception) { 
            Logger.warning(TAG, "Failed to read HTTP error response body: ${bodyException.message}")
            "Unable to read response body" 
        }
        return "[$statusCode $statusText] $body"
    }
}
