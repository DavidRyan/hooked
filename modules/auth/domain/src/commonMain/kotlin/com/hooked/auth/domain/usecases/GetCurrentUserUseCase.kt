package com.hooked.auth.domain.usecases

import com.hooked.auth.domain.entities.UserEntity
import com.hooked.auth.domain.repositories.AuthRepository
import com.hooked.core.domain.UseCaseResult

class GetCurrentUserUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): UseCaseResult<UserEntity?> {
        return try {
            val result = authRepository.getCurrentUser()
            if (result.isSuccess) {
                UseCaseResult.Success(result.getOrNull())
            } else {
                val exception = result.exceptionOrNull()
                UseCaseResult.Error(
                    exception?.message ?: "Failed to get current user",
                    exception,
                    "GetCurrentUserUseCase"
                )
            }
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Failed to get current user", e, "GetCurrentUserUseCase")
        }
    }
}