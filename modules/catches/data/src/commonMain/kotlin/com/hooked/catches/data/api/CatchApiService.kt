package com.hooked.catches.data.api

import com.hooked.catches.data.model.CatchDto
import com.hooked.core.domain.NetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class CatchApiService(
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
}