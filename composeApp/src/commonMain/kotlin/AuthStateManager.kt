package com.hooked

import androidx.compose.runtime.*
import com.hooked.auth.domain.usecases.CheckAuthStatusUseCase
import com.hooked.auth.domain.usecases.GetCurrentUserUseCase
import com.hooked.core.domain.UseCaseResult
import com.hooked.core.logging.Logger
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AuthStateManager(
    onAuthenticatedAndOnboarded: () -> Unit,
    onAuthenticatedNeedsOnboarding: () -> Unit,
    onUnauthenticatedUser: () -> Unit
) {
    val checkAuthStatusUseCase: CheckAuthStatusUseCase = koinInject()
    val getCurrentUserUseCase: GetCurrentUserUseCase = koinInject()
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        launch {
            try {
                val isAuthenticated = checkAuthStatusUseCase()
                if (!isAuthenticated) {
                    onUnauthenticatedUser()
                } else {
                    when (val userResult = getCurrentUserUseCase()) {
                        is UseCaseResult.Success -> {
                            val onboarded = userResult.data?.onboardingCompleted ?: false
                            if (onboarded) onAuthenticatedAndOnboarded()
                            else onAuthenticatedNeedsOnboarding()
                        }
                        is UseCaseResult.Error -> {
                            // Authenticated but couldn't fetch user data — proceed to main
                            // rather than block the user behind onboarding.
                            Logger.warning("AuthStateManager", "Auth ok but user fetch failed: ${userResult.message}")
                            onAuthenticatedAndOnboarded()
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.error("AuthStateManager", "Failed to check auth status: ${e.message}", e)
                onUnauthenticatedUser()
            } finally {
                isLoading = false
            }
        }
    }
}
