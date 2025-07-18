package com.hooked.auth.data.repositories

import com.hooked.auth.data.datasources.AuthDataSource
import com.hooked.auth.data.datasources.RemoteAuthDataSource
import com.hooked.auth.domain.entities.LoginCredentials
import com.hooked.auth.domain.entities.RegisterCredentials
import com.hooked.auth.domain.entities.UserEntity
import com.hooked.auth.domain.repositories.AuthRepository

class AuthRepositoryImpl(
    private val authDataSource: AuthDataSource
) : AuthRepository {
    
    override suspend fun login(credentials: LoginCredentials): Result<UserEntity> {
        return authDataSource.login(credentials)
    }
    
    override suspend fun register(credentials: RegisterCredentials): Result<UserEntity> {
        return authDataSource.register(credentials)
    }
    
    override suspend fun logout(): Result<Unit> {
        return authDataSource.logout()
    }
    
    override suspend fun getCurrentUser(): Result<UserEntity?> {
        return authDataSource.getCurrentUser()
    }
    
    override suspend fun refreshToken(): Result<UserEntity> {
        return if (authDataSource is RemoteAuthDataSource) {
            authDataSource.refreshToken()
        } else {
            Result.failure(Exception("Token refresh not supported"))
        }
    }
    
    override suspend fun isLoggedIn(): Boolean {
        return try {
            val result = authDataSource.getCurrentUser()
            result.isSuccess && result.getOrNull() != null
        } catch (e: Exception) {
            false
        }
    }
}