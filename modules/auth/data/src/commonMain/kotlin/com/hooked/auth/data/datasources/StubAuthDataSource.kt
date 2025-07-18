package com.hooked.auth.data.datasources

import com.hooked.auth.domain.entities.LoginCredentials
import com.hooked.auth.domain.entities.RegisterCredentials
import com.hooked.auth.domain.entities.UserEntity
import kotlinx.coroutines.delay

class StubAuthDataSource : AuthDataSource {
    private var currentUser: UserEntity? = null
    
    override suspend fun login(credentials: LoginCredentials): Result<UserEntity> {
        delay(1000)
        
        return if (credentials.email == "test@example.com" && credentials.password == "password") {
            val user = UserEntity(
                id = "1",
                email = credentials.email,
                username = "testuser",
                token = "stub_token_123"
            )
            currentUser = user
            Result.success(user)
        } else {
            Result.failure(Exception("Invalid credentials"))
        }
    }
    
    override suspend fun register(credentials: RegisterCredentials): Result<UserEntity> {
        delay(1000)
        
        val user = UserEntity(
            id = "2",
            email = credentials.email,
            username = "${credentials.firstName} ${credentials.lastName}",
            token = "stub_token_456"
        )
        currentUser = user
        return Result.success(user)
    }
    
    override suspend fun logout(): Result<Unit> {
        delay(500)
        currentUser = null
        return Result.success(Unit)
    }
    
    override suspend fun getCurrentUser(): Result<UserEntity?> {
        return Result.success(currentUser)
    }
    
    override suspend fun saveUser(user: UserEntity): Result<Unit> {
        currentUser = user
        return Result.success(Unit)
    }
    
    override suspend fun clearUser(): Result<Unit> {
        currentUser = null
        return Result.success(Unit)
    }
}