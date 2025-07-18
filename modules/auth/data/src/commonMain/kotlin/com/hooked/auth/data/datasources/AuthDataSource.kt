package com.hooked.auth.data.datasources

import com.hooked.auth.domain.entities.LoginCredentials
import com.hooked.auth.domain.entities.RegisterCredentials
import com.hooked.auth.domain.entities.UserEntity

interface AuthDataSource {
    suspend fun login(credentials: LoginCredentials): Result<UserEntity>
    suspend fun register(credentials: RegisterCredentials): Result<UserEntity>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): Result<UserEntity?>
    suspend fun saveUser(user: UserEntity): Result<Unit>
    suspend fun clearUser(): Result<Unit>
}