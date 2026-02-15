package com.hooked.auth.presentation

import com.hooked.auth.domain.usecases.GetCurrentUserUseCase
import com.hooked.auth.domain.usecases.LogoutUseCase
import com.hooked.auth.presentation.model.ProfileEffect
import com.hooked.auth.presentation.model.ProfileIntent
import com.hooked.auth.presentation.model.ProfileState
import com.hooked.core.HookedViewModel
import com.hooked.core.domain.UseCaseResult
import com.hooked.core.logging.Logger
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase
) : HookedViewModel<ProfileIntent, ProfileState, ProfileEffect>() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    override fun createInitialState(): ProfileState = ProfileState()

    override fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.LoadProfile -> loadProfile()
            is ProfileIntent.Logout -> performLogout()
        }
    }

    private fun loadProfile() {
        setState { copy(isLoading = true) }

        viewModelScope.launch {
            when (val result = getCurrentUserUseCase()) {
                is UseCaseResult.Success -> {
                    val user = result.data
                    if (user != null) {
                        setState {
                            copy(
                                username = user.username,
                                email = user.email,
                                initials = deriveInitials(user.username),
                                isLoading = false
                            )
                        }
                    } else {
                        setState { copy(isLoading = false) }
                        sendEffect { ProfileEffect.ShowError("User not found") }
                    }
                }
                is UseCaseResult.Error -> {
                    Logger.error(TAG, "Failed to load profile: ${result.message}")
                    setState { copy(isLoading = false) }
                    sendEffect { ProfileEffect.ShowError(result.message) }
                }
            }
        }
    }

    private fun performLogout() {
        setState { copy(isLoggingOut = true) }

        viewModelScope.launch {
            when (val result = logoutUseCase()) {
                is UseCaseResult.Success -> {
                    sendEffect { ProfileEffect.NavigateToLogin }
                }
                is UseCaseResult.Error -> {
                    Logger.error(TAG, "Logout failed: ${result.message}")
                    setState { copy(isLoggingOut = false) }
                    sendEffect { ProfileEffect.ShowError(result.message) }
                }
            }
        }
    }

    private fun deriveInitials(username: String): String {
        val parts = username.trim().split("\\s+".toRegex())
        return when {
            parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}"
            parts.isNotEmpty() && parts.first().isNotEmpty() -> parts.first().first().toString()
            else -> "?"
        }.uppercase()
    }
}
