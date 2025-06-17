package com.hooked.submit.data.api

import com.hooked.submit.data.model.SubmitCatchDto
import com.hooked.core.domain.NetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SubmitApiService(
    private val httpClient: HttpClient,
) {
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
            return NetworkResult.Error(e, "SubmitApiService.submitCatch")
        }
    }
}