package com.hooked.auth.domain.usecases

import com.hooked.auth.domain.entities.LoginCredentials
import com.hooked.auth.domain.entities.UserEntity
import com.hooked.auth.domain.repositories.AuthRepository
import com.hooked.core.domain.UseCaseResult

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(credentials: LoginCredentials): UseCaseResult<UserEntity> {
        return try {
            val result = authRepository.login(credentials)
            if (result.isSuccess) {
                UseCaseResult.Success(result.getOrThrow())
            } else {
                val exception = result.exceptionOrNull()
                UseCaseResult.Error(
                    exception?.message ?: "Login failed",
                    exception,
                    "LoginUseCase"
                )
            }
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Login failed", e, "LoginUseCase")
        }
    }
}