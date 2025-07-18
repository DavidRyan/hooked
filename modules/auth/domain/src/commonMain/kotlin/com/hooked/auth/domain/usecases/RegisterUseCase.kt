package com.hooked.auth.domain.usecases

import com.hooked.auth.domain.entities.RegisterCredentials
import com.hooked.auth.domain.entities.UserEntity
import com.hooked.auth.domain.repositories.AuthRepository
import com.hooked.core.domain.UseCaseResult

class RegisterUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(credentials: RegisterCredentials): UseCaseResult<UserEntity> {
        return try {
            val result = authRepository.register(credentials)
            if (result.isSuccess) {
                UseCaseResult.Success(result.getOrThrow())
            } else {
                val exception = result.exceptionOrNull()
                UseCaseResult.Error(
                    exception?.message ?: "Registration failed",
                    exception,
                    "RegisterUseCase"
                )
            }
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Registration failed", e, "RegisterUseCase")
        }
    }
}