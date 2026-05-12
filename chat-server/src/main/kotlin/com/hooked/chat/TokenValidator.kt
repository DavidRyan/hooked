package com.hooked.chat

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Validates a Hooked JWT by calling the Hooked backend's `/auth/me` endpoint.
 *
 * Avoids the need to share a JWT secret between chat-server and the Phoenix backend —
 * we delegate validation to the source of truth.
 */
class TokenValidator(private val hookedApiUrl: String) {
    data class UserInfo(val id: String, val email: String)

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    fun validate(token: String): UserInfo? {
        if (token.isBlank()) return null

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$hookedApiUrl/auth/me"))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer $token")
            .GET()
            .build()

        return try {
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                System.err.println("[chat-server] /auth/me returned ${response.statusCode()}")
                return null
            }

            val parsed = json.decodeFromString<MeResponse>(response.body())
            UserInfo(id = parsed.data.id, email = parsed.data.email)
        } catch (e: Exception) {
            System.err.println("[chat-server] /auth/me failed: ${e.message}")
            null
        }
    }

    @Serializable
    private data class MeResponse(val data: MeUser)

    @Serializable
    private data class MeUser(val id: String, val email: String)
}
