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
        return try {
            val remoteCatches = httpClient
                .get("http://10.0.2.2:8080/catches")
                .body<List<CatchDto>>()
            NetworkResult.Success(remoteCatches)
        } catch (e: Exception) {
            NetworkResult.Error(e, "CatchApiService.getCatches")
        }
    }
    
    suspend fun getCatchDetails(catchId: Long): NetworkResult<CatchDto> {
        try {
            val catchDetails = httpClient
                .get("http://10.0.2.2:8080/catches/$catchId")
                .body<CatchDto>()
            return NetworkResult.Success(catchDetails)
        } catch (e: Exception) {
            return NetworkResult.Error(e, "CatchApiService.getCatchDetails")
        }
    }
}