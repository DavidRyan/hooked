package com.hooked.skunks.data.api

import com.hooked.core.config.NetworkConfig
import com.hooked.skunks.data.model.SkunkDto
import com.hooked.skunks.data.model.SubmitSkunkBody
import com.hooked.skunks.data.model.SubmitSkunkRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SkunkApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String = NetworkConfig.BASE_URL
) {
    suspend fun submitSkunk(
        fishedAt: String,
        latitude: Double?,
        longitude: Double?,
        notes: String?
    ): String {
        val request = SubmitSkunkRequest(
            userSkunk = SubmitSkunkBody(
                fishedAt = fishedAt,
                latitude = latitude,
                longitude = longitude,
                notes = notes
            )
        )

        val response: Map<String, SkunkDto> = httpClient.post("$baseUrl/user_skunks") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        return response["user_skunk"]?.id
            ?: throw IllegalStateException("No user_skunk in response")
    }
}
