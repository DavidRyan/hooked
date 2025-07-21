package com.hooked.auth.data.api

import com.hooked.auth.data.model.AuthErrorDto
import com.hooked.auth.data.model.AuthResponseDto
import com.hooked.auth.data.model.LoginRequestDto
import com.hooked.auth.data.model.RegisterRequestDto
import com.hooked.auth.data.model.UserResponseDto
import com.hooked.core.config.NetworkConfig
import com.hooked.core.domain.NetworkResult
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
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class AuthApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String = NetworkConfig.BASE_URL
) {
    
    suspend fun login(email: String, password: String): NetworkResult<AuthResponseDto> {
        return try {
            val response = httpClient.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequestDto(email = email, password = password))
            }.body<AuthResponseDto>()
            
            NetworkResult.Success(response)
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> {
                    try {
                        val errorResponse = e.response.body<AuthErrorDto>()
                        val detailedMessage = buildDetailedErrorMessage(errorResponse)
                        NetworkResult.Error(Exception(detailedMessage), "AuthApiService.login")
                    } catch (parseError: Exception) {
                        val rawResponse = try { e.response.bodyAsText() } catch (_: Exception) { "Unable to read response body" }
                        val errorMessage = "Login failed - Parse error: ${parseError.message}, Raw response: $rawResponse"
                        NetworkResult.Error(Exception(errorMessage), "AuthApiService.login")
                    }
                }
                else -> NetworkResult.Error(e, "AuthApiService.login")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e, "AuthApiService.login")
        }
    }
    
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): NetworkResult<AuthResponseDto> {
        return try {
            val response = httpClient.post("$baseUrl/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequestDto(
                    user = com.hooked.auth.data.model.UserRegistrationDto(
                        email = email,
                        password = password,
                        firstName = firstName,
                        lastName = lastName
                    )
                ))
            }.body<AuthResponseDto>()
            
            NetworkResult.Success(response)
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.UnprocessableEntity -> {
                    try {
                        val errorResponse = e.response.body<AuthErrorDto>()
                        val detailedMessage = buildDetailedErrorMessage(errorResponse)
                        NetworkResult.Error(Exception(detailedMessage), "AuthApiService.register")
                    } catch (parseError: Exception) {
                        // Log the parsing error and raw response for debugging
                        val rawResponse = try { e.response.bodyAsText() } catch (_: Exception) { "Unable to read response body" }
                        val errorMessage = "Registration failed - Parse error: ${parseError.message}, Raw response: $rawResponse"
                        NetworkResult.Error(Exception(errorMessage), "AuthApiService.register")
                    }
                }
                else -> NetworkResult.Error(e, "AuthApiService.register")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e, "AuthApiService.register")
        }
    }
    
    suspend fun getCurrentUser(token: String): NetworkResult<UserResponseDto> {
        return try {
            val response = httpClient.get("$baseUrl/auth/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body<UserResponseDto>()
            
            NetworkResult.Success(response)
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> {
                    NetworkResult.Error(Exception("Token expired or invalid"), "AuthApiService.getCurrentUser")
                }
                else -> NetworkResult.Error(e, "AuthApiService.getCurrentUser")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e, "AuthApiService.getCurrentUser")
        }
    }
    
    suspend fun refreshToken(token: String): NetworkResult<AuthResponseDto> {
        return try {
            val response = httpClient.post("$baseUrl/auth/refresh") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body<AuthResponseDto>()
            
            NetworkResult.Success(response)
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> {
                    NetworkResult.Error(Exception("Token expired or invalid"), "AuthApiService.refreshToken")
                }
                else -> NetworkResult.Error(e, "AuthApiService.refreshToken")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e, "AuthApiService.refreshToken")
        }
    }
    
    private fun buildDetailedErrorMessage(errorResponse: AuthErrorDto): String {
        val baseMessage = errorResponse.message ?: errorResponse.error
        
        return if (errorResponse.details != null && errorResponse.details.isNotEmpty()) {
            val fieldErrors = errorResponse.details.entries.joinToString("; ") { (field, messages) ->
                "$field: ${messages.joinToString(", ")}"
            }
            "$baseMessage - $fieldErrors"
        } else {
            baseMessage
        }
    }
}