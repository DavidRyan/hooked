package com.hooked.auth.domain.usecases

import com.hooked.auth.domain.entities.UserEntity
import com.hooked.auth.domain.repositories.AuthRepository
import com.hooked.core.domain.UseCaseResult

class RefreshTokenUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): UseCaseResult<UserEntity> {
        return try {
            val result = authRepository.refreshToken()
            if (result.isSuccess) {
                UseCaseResult.Success(result.getOrThrow())
            } else {
                val exception = result.exceptionOrNull()
                UseCaseResult.Error(
                    exception?.message ?: "Token refresh failed",
                    exception,
                    "RefreshTokenUseCase"
                )
            }
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Token refresh failed", e, "RefreshTokenUseCase")
        }
    }
}