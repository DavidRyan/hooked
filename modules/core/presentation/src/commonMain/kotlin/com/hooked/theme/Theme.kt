package com.hooked.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HookedTheme = darkColorScheme(
    primary = Colors.deepOcean,
    secondary = Colors.coral,
    tertiary = Colors.seaFoam,
    background = Colors.base,
    surface = Colors.surface0,
    onPrimary = Colors.pearl,
    onSecondary = Colors.crust,
    onTertiary = Colors.crust,
    onBackground = Colors.text,
    onSurface = Colors.text
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