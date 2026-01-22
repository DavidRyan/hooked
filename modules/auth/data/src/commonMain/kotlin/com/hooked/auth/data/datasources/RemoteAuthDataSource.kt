package com.hooked.auth.data.datasources

import com.hooked.auth.data.api.AuthApiService
import com.hooked.auth.data.model.toUserEntity
import com.hooked.auth.data.storage.TokenStorage
import com.hooked.auth.domain.entities.LoginCredentials
import com.hooked.auth.domain.entities.RegisterCredentials
import com.hooked.auth.domain.entities.UserEntity
import com.hooked.core.domain.NetworkResult
import com.hooked.core.logging.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RemoteAuthDataSource(
    private val authApiService: AuthApiService,
    private val tokenStorage: TokenStorage,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : AuthDataSource {
    
    override suspend fun login(credentials: LoginCredentials): Result<UserEntity> {
        return when (val result = authApiService.login(credentials.email, credentials.password)) {
            is NetworkResult.Success -> {
                val userEntity = result.data.toUserEntity()
                
                // Store token and user data
                tokenStorage.saveToken(userEntity.token ?: "")
                tokenStorage.saveUser(json.encodeToString(userEntity))
                
                Result.success(userEntity)
            }
            is NetworkResult.Error -> {
                Result.failure(result.error)
            }
            is NetworkResult.Loading -> {
                Result.failure(Exception("Unexpected loading state"))
            }
        }
    }
    
    override suspend fun register(credentials: RegisterCredentials): Result<UserEntity> {
        return when (val result = authApiService.register(
            credentials.email,
            credentials.password,
            credentials.firstName,
            credentials.lastName
        )) {
            is NetworkResult.Success -> {
                val userEntity = result.data.toUserEntity()
                
                // Store token and user data
                tokenStorage.saveToken(userEntity.token ?: "")
                tokenStorage.saveUser(json.encodeToString(userEntity))
                
                Result.success(userEntity)
            }
            is NetworkResult.Error -> {
                Result.failure(result.error)
            }
            is NetworkResult.Loading -> {
                Result.failure(Exception("Unexpected loading state"))
            }
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            tokenStorage.clearToken()
            tokenStorage.clearUser()
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.error("RemoteAuthDataSource", "Failed to logout: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getCurrentUser(): Result<UserEntity?> {
        return try {
            val userJson = tokenStorage.getUser()
            if (userJson != null) {
                val user = json.decodeFromString<UserEntity>(userJson)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            // If we can't parse stored user, try to fetch from API
            Logger.warning("RemoteAuthDataSource", "Failed to parse stored user JSON: ${e.message}", e)
            val token = tokenStorage.getToken()
            if (token != null) {
                when (val result = authApiService.getCurrentUser(token)) {
                    is NetworkResult.Success -> {
                        val userEntity = result.data.toUserEntity().copy(token = token)
                        tokenStorage.saveUser(json.encodeToString(userEntity))
                        Result.success(userEntity)
                    }
                    is NetworkResult.Error -> {
                        // Token might be expired, clear storage
                        tokenStorage.clearToken()
                        tokenStorage.clearUser()
                        Result.success(null)
                    }
                    is NetworkResult.Loading -> {
                        Result.failure(Exception("Unexpected loading state"))
                    }
                }
            } else {
                Result.success(null)
            }
        }
    }
    
    override suspend fun saveUser(user: UserEntity): Result<Unit> {
        return try {
            tokenStorage.saveUser(json.encodeToString(user))
            user.token?.let { tokenStorage.saveToken(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.error("RemoteAuthDataSource", "Failed to save user: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun clearUser(): Result<Unit> {
        return try {
            tokenStorage.clearToken()
            tokenStorage.clearUser()
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.error("RemoteAuthDataSource", "Failed to clear user: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun refreshToken(): Result<UserEntity> {
        val currentToken = tokenStorage.getToken()
            ?: return Result.failure(Exception("No token available"))
            
        return when (val result = authApiService.refreshToken(currentToken)) {
            is NetworkResult.Success -> {
                val userEntity = result.data.toUserEntity()
                
                // Store new token and user data
                tokenStorage.saveToken(userEntity.token ?: "")
                tokenStorage.saveUser(json.encodeToString(userEntity))
                
                Result.success(userEntity)
            }
            is NetworkResult.Error -> {
                // Token refresh failed, clear storage
                tokenStorage.clearToken()
                tokenStorage.clearUser()
                Result.failure(result.error)
            }
            is NetworkResult.Loading -> {
                Result.failure(Exception("Unexpected loading state"))
            }
        }
    }
}