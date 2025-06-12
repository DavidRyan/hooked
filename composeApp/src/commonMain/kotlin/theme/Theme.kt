package theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HookedTheme = darkColorScheme(
    primary = Colors.blue,
    secondary = Colors.mauve,
    tertiary = Colors.teal,
    background = Colors.base,
    surface = Colors.surface0,
    onPrimary = Colors.crust,
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