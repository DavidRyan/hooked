package com.hooked.auth.domain.usecases

import com.hooked.auth.domain.repositories.AuthRepository
import com.hooked.core.domain.UseCaseResult

class LogoutUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): UseCaseResult<Unit> {
        return try {
            val result = authRepository.logout()
            if (result.isSuccess) {
                UseCaseResult.Success(Unit)
            } else {
                val exception = result.exceptionOrNull()
                UseCaseResult.Error(
                    exception?.message ?: "Logout failed",
                    exception,
                    "LogoutUseCase"
                )
            }
        } catch (e: Exception) {
            UseCaseResult.Error(e.message ?: "Logout failed", e, "LogoutUseCase")
        }
    }
}