package com.hooked.auth.data.api

import com.hooked.auth.data.model.AuthResponseDto
import com.hooked.auth.data.model.LoginRequestDto
import com.hooked.auth.data.model.RegisterRequestDto
import com.hooked.auth.data.model.UserResponseDto
import com.hooked.core.config.NetworkConfig
import com.hooked.core.domain.NetworkResult
import com.hooked.core.logging.Logger
import com.hooked.core.logging.logRequest
import com.hooked.core.logging.logResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class AuthApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String = NetworkConfig.BASE_URL
) {
    companion object {
        private const val TAG = "AuthApiService"
    }
    
    suspend fun login(email: String, password: String): NetworkResult<AuthResponseDto> {
        val endpoint = "/auth/login"
        Logger.logRequest(TAG, "POST", endpoint)
        return try {
            val response = httpClient.post("$baseUrl$endpoint") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequestDto(email = email, password = password))
            }.body<AuthResponseDto>()
            
            Logger.logResponse(TAG, 200, "OK - Login successful")
            NetworkResult.Success(response)
        } catch (e: ClientRequestException) {
            NetworkResult.Error(Exception(formatHttpError(e)), TAG)
        } catch (e: Exception) {
            NetworkResult.Error(Exception("Login failed: ${e.message}", e), TAG)
        }
    }
    
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): NetworkResult<AuthResponseDto> {
        val endpoint = "/auth/register"
        Logger.logRequest(TAG, "POST", endpoint)
        return try {
            val response = httpClient.post("$baseUrl$endpoint") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequestDto(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName
                ))
            }.body<AuthResponseDto>()
            
            Logger.logResponse(TAG, 201, "Created - Registration successful")
            NetworkResult.Success(response)
        } catch (e: ClientRequestException) {
            NetworkResult.Error(Exception(formatHttpError(e)), TAG)
        } catch (e: Exception) {
            NetworkResult.Error(Exception("Registration failed: ${e.message}", e), TAG)
        }
    }
    
    suspend fun getCurrentUser(token: String): NetworkResult<UserResponseDto> {
        val endpoint = "/auth/me"
        Logger.logRequest(TAG, "GET", endpoint)
        return try {
            val response = httpClient.get("$baseUrl$endpoint") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body<UserResponseDto>()
            
            Logger.logResponse(TAG, 200, "OK")
            NetworkResult.Success(response)
        } catch (e: ClientRequestException) {
            NetworkResult.Error(Exception(formatHttpError(e)), TAG)
        } catch (e: Exception) {
            NetworkResult.Error(Exception("Failed to get current user: ${e.message}", e), TAG)
        }
    }
    
    suspend fun refreshToken(token: String): NetworkResult<AuthResponseDto> {
        val endpoint = "/auth/refresh"
        Logger.logRequest(TAG, "POST", endpoint)
        return try {
            val response = httpClient.post("$baseUrl$endpoint") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body<AuthResponseDto>()
            
            Logger.logResponse(TAG, 200, "OK - Token refreshed")
            NetworkResult.Success(response)
        } catch (e: ClientRequestException) {
            NetworkResult.Error(Exception(formatHttpError(e)), TAG)
        } catch (e: Exception) {
            NetworkResult.Error(Exception("Token refresh failed: ${e.message}", e), TAG)
        }
    }
    
    private suspend fun formatHttpError(e: ClientRequestException): String {
        val statusCode = e.response.status.value
        val statusText = e.response.status.description
        val body = try { e.response.bodyAsText() } catch (_: Exception) { "Unable to read response body" }
        return "[$statusCode $statusText] $body"
    }
}
