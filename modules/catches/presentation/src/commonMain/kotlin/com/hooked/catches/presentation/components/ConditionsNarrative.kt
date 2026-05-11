package com.hooked.catches.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.hooked.theme.Colors
import kotlin.math.roundToInt

@Composable
fun ConditionsNarrative(
    weatherData: Map<String, String?>?,
    modifier: Modifier = Modifier
) {
    val sentence = buildSentence(weatherData) ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Conditions",
            style = MaterialTheme.typography.labelLarge,
            color = Colors.subtext0
        )
        Text(
            text = sentence,
            style = MaterialTheme.typography.bodyLarge,
            color = Colors.text,
            fontStyle = FontStyle.Italic
        )
    }
}

private fun buildSentence(weather: Map<String, String?>?): String? {
    if (weather.isNullOrEmpty()) return null

    val parts = mutableListOf<String>()

    weather["temp"]?.toFloatOrNull()?.let { parts += "${it.roundToInt()}°" }
    weather["description"]?.takeIf { it.isNotBlank() }?.let { parts += it.lowercase() }

    val wind = weather["wind_speed"]?.toFloatOrNull()
    val dir = weather["wind_direction"]?.toFloatOrNull()
    when {
        wind != null && dir != null ->
            parts += "wind ${wind.roundToInt()} from ${compassDirection(dir)}"
        wind != null ->
            parts += "wind ${wind.roundToInt()}"
    }

    weather["humidity"]?.toFloatOrNull()?.let { parts += "${it.roundToInt()}% humidity" }

    if (parts.isEmpty()) return null
    return parts.joinToString(", ").replaceFirstChar { it.uppercase() } + "."
}

private fun compassDirection(degrees: Float): String {
    val d = ((degrees % 360f) + 360f) % 360f
    return when {
        d < 22.5f || d >= 337.5f -> "N"
        d < 67.5f -> "NE"
        d < 112.5f -> "E"
        d < 157.5f -> "SE"
        d < 202.5f -> "S"
        d < 247.5f -> "SW"
        d < 292.5f -> "W"
        else -> "NW"
    }
}
