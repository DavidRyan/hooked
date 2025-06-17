package com.hooked.core.util

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // On iOS, back navigation is handled by the system's swipe gesture
    // No explicit back handler is needed
}