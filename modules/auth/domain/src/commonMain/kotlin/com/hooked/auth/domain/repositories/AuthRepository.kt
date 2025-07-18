package com.hooked.auth.domain.repositories

import com.hooked.auth.domain.entities.LoginCredentials
import com.hooked.auth.domain.entities.RegisterCredentials
import com.hooked.auth.domain.entities.UserEntity

interface AuthRepository {
    suspend fun login(credentials: LoginCredentials): Result<UserEntity>
    suspend fun register(credentials: RegisterCredentials): Result<UserEntity>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): Result<UserEntity?>
    suspend fun refreshToken(): Result<UserEntity>
    suspend fun isLoggedIn(): Boolean
}