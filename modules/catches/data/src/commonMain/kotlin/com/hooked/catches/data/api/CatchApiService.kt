package com.hooked.catches.data.api

import com.hooked.catches.data.model.CatchDto
import com.hooked.catches.data.model.SubmitCatchDto
import com.hooked.core.config.NetworkConfig
import com.hooked.core.domain.NetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class CatchApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String = NetworkConfig.BASE_URL
) {
    suspend fun getCatches(): NetworkResult<List<CatchDto>> {
        return try {
            com.hooked.core.logging.Logger.debug("CatchApiService", "getCatches URL: $baseUrl/user_catches")
            val response = httpClient
                .get("$baseUrl/user_catches")
                .body<Map<String, List<CatchDto>>>()
            
            val catches = response["user_catches"] ?: emptyList()
            NetworkResult.Success(catches)
        } catch (e: Exception) {
            val detailedMessage = "Failed to fetch catches: ${e.message}"
            NetworkResult.Error(Exception(detailedMessage, e), "CatchApiService.getCatches")
        }
    }
    
    suspend fun getCatchDetails(catchId: String): NetworkResult<CatchDto> {
        return try {
            val response = httpClient
                .get("$baseUrl/user_catches/$catchId")
                .body<Map<String, CatchDto>>()
            
            val catchDetails = response["user_catch"] 
                ?: return NetworkResult.Error(Exception("Catch not found"), "CatchApiService.getCatchDetails")
            
            NetworkResult.Success(catchDetails)
        } catch (e: Exception) {
            val detailedMessage = "Failed to fetch catch details for ID $catchId: ${e.message}"
            NetworkResult.Error(Exception(detailedMessage, e), "CatchApiService.getCatchDetails")
        }
    }
    
    suspend fun submitCatch(submitCatchDto: SubmitCatchDto, imageBytes: ByteArray? = null): NetworkResult<String> {
        return try {
            com.hooked.core.logging.Logger.debug("CatchApiService", "submitCatch URL: $baseUrl/user_catches")
            val response = httpClient.post("$baseUrl/user_catches") {
                setBody(MultiPartFormDataContent(
                    formData {
                        // Add catch data fields
                        submitCatchDto.species?.let { append("user_catch[species]", it) }
                        submitCatchDto.location?.let { append("user_catch[location]", it) }
                        submitCatchDto.caughtAt?.let { append("user_catch[caught_at]", it) }
                        submitCatchDto.latitude?.let { append("user_catch[latitude]", it.toString()) }
                        submitCatchDto.longitude?.let { append("user_catch[longitude]", it.toString()) }
                        submitCatchDto.notes?.let { append("user_catch[notes]", it) }

                        // Add image if provided
                        imageBytes?.let { bytes ->
                            append("image", bytes, Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"catch.jpg\"")
                            })
                        }
                    }
                ))
            }.body<Map<String, CatchDto>>()
            
            val userCatch = response["user_catch"] 
                ?: return NetworkResult.Error(Exception("No catch returned"), "CatchApiService.submitCatch")
            
            NetworkResult.Success(userCatch.id)
        } catch (e: Exception) {
            val detailedMessage = "Failed to submit catch: ${e.message}"
            NetworkResult.Error(Exception(detailedMessage, e), "CatchApiService.submitCatch")
        }
    }
    
    suspend fun deleteCatch(catchId: String): NetworkResult<Unit> {
        return try {
            com.hooked.core.logging.Logger.debug("CatchApiService", "deleteCatch URL: $baseUrl/user_catches/$catchId")
            httpClient.delete("$baseUrl/user_catches/$catchId")
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            val detailedMessage = "Failed to delete catch: ${e.message}"
            NetworkResult.Error(Exception(detailedMessage, e), "CatchApiService.deleteCatch")
        }
    }
    
    suspend fun getAiInsights(): NetworkResult<String> {
        return try {
            com.hooked.core.logging.Logger.debug("CatchApiService", "getAiInsights URL: $baseUrl/ai/insights")
            val response = httpClient
                .get("$baseUrl/ai/insights")
                .body<Map<String, String>>()
            
            val insights = response["insights"] 
                ?: return NetworkResult.Error(Exception("No insights returned"), "CatchApiService.getAiInsights")
            
            NetworkResult.Success(insights)
        } catch (e: Exception) {
            val detailedMessage = "Failed to fetch AI insights: ${e.message}"
            NetworkResult.Error(Exception(detailedMessage, e), "CatchApiService.getAiInsights")
        }
    }
}