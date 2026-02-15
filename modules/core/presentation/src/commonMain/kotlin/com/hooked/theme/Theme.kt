package com.hooked.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HookedTheme = darkColorScheme(
    // Primary colors
    primary = Colors.mediumTeal,
    onPrimary = Colors.darkNavy,
    primaryContainer = Colors.lightMint,
    onPrimaryContainer = Colors.darkNavy,

    // Secondary colors
    secondary = Colors.lightMint,
    onSecondary = Colors.darkNavy,
    secondaryContainer = Colors.lightMint,
    onSecondaryContainer = Colors.darkNavy,

    // Tertiary colors
    tertiary = Colors.mediumBlue,
    onTertiary = Colors.darkNavy,
    tertiaryContainer = Colors.surface2,
    onTertiaryContainer = Colors.text,
    
    // Error colors
    error = Colors.error,
    onError = Colors.white,
    errorContainer = Colors.red,
    onErrorContainer = Colors.white,
    
    // Background colors
    background = Colors.base,
    onBackground = Colors.text,
    
    // Surface colors
    surface = Colors.surface0,
    onSurface = Colors.text,
    surfaceVariant = Colors.surface1,
    onSurfaceVariant = Colors.subtext1,
    surfaceTint = Colors.mediumTeal,

    // Inverse colors (for snackbars, etc.)
    inverseSurface = Colors.lightBlue,
    inverseOnSurface = Colors.darkNavy,
    inversePrimary = Colors.mediumTeal,
    
    // Outline colors (borders, dividers)
    outline = Colors.overlay1,
    outlineVariant = Colors.overlay0,
    
    // Scrim (overlay backgrounds for modals/dialogs)
    scrim = Color.Black.copy(alpha = 0.5f)
)

@Composable
fun HookedTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HookedTheme,
        typography = Typography(),
        content = content
    )
}