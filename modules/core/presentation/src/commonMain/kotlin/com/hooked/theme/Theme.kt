package com.hooked.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HookedTheme = darkColorScheme(
    primary = Colors.primary,
    onPrimary = Colors.onPrimary,
    primaryContainer = Colors.surface2,
    onPrimaryContainer = Colors.text,

    secondary = Colors.secondary,
    onSecondary = Colors.onSecondary,
    secondaryContainer = Colors.surface2,
    onSecondaryContainer = Colors.text,

    tertiary = Colors.tertiary,
    onTertiary = Colors.onTertiary,
    tertiaryContainer = Colors.surface2,
    onTertiaryContainer = Colors.text,

    error = Colors.error,
    onError = Colors.text,
    errorContainer = Colors.error,
    onErrorContainer = Colors.text,

    background = Colors.base,
    onBackground = Colors.text,

    surface = Colors.surface1,
    onSurface = Colors.text,
    surfaceVariant = Colors.surface2,
    onSurfaceVariant = Colors.subtext1,
    // Disable M3's auto-tint-on-elevation; we control surface gradations ourselves.
    surfaceTint = Color.Transparent,

    inverseSurface = Colors.text,
    inverseOnSurface = Colors.base,
    inversePrimary = Colors.primary,

    outline = Colors.overlay1,
    outlineVariant = Colors.overlay0,

    scrim = Color.Black.copy(alpha = 0.6f)
)

@Composable
fun HookedTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HookedTheme,
        typography = HookedTypography(),
        content = content
    )
}
