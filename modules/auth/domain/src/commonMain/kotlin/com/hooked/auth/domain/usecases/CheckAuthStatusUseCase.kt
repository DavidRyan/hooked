package com.hooked.auth.domain.usecases

import com.hooked.auth.domain.repositories.AuthRepository

class CheckAuthStatusUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Boolean {
        return authRepository.isLoggedIn()
    }
}