package com.hooked.auth.domain.usecases

import com.hooked.auth.domain.entities.OnboardingPreferences
import com.hooked.auth.domain.entities.UserEntity
import com.hooked.auth.domain.repositories.AuthRepository

class UpdatePreferencesUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(prefs: OnboardingPreferences): Result<UserEntity> {
        return authRepository.updatePreferences(prefs)
    }
}
