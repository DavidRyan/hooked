package com.hooked

import androidx.compose.runtime.*
import com.hooked.auth.domain.usecases.CheckAuthStatusUseCase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AuthStateManager(
    onAuthenticatedUser: () -> Unit,
    onUnauthenticatedUser: () -> Unit
) {
    val checkAuthStatusUseCase: CheckAuthStatusUseCase = koinInject()
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        launch {
            try {
                val isAuthenticated = checkAuthStatusUseCase()
                if (isAuthenticated) {
                    onAuthenticatedUser()
                } else {
                    onUnauthenticatedUser()
                }
            } catch (e: Exception) {
                // If there's an error checking auth status, assume unauthenticated
                onUnauthenticatedUser()
            } finally {
                isLoading = false
            }
        }
    }
    
    if (isLoading) {
        // Simple loading indicator
        // You can replace this with a proper splash screen
    }
}