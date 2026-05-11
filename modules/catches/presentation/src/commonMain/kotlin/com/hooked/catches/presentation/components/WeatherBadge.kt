package com.hooked.catches.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun WeatherBadge(
    description: String?,
    tempFahrenheit: Float?,
    modifier: Modifier = Modifier,
    background: Color = Color.Black.copy(alpha = 0.45f),
    tint: Color = Color.White
) {
    if (description == null && tempFahrenheit == null) return

    val (icon, label) = iconAndLabel(description)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        val textParts = buildList {
            tempFahrenheit?.let { add("${it.roundToInt()}°") }
            label.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        if (textParts.isNotEmpty()) {
            Text(
                text = textParts.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = tint
            )
        }
    }
}

private fun iconAndLabel(description: String?): Pair<ImageVector, String> {
    val d = description?.lowercase() ?: return Icons.Filled.Cloud to ""
    return when {
        d.contains("thunder") || d.contains("storm") -> Icons.Filled.Bolt to "Storms"
        d.contains("snow") -> Icons.Filled.AcUnit to "Snow"
        d.contains("rain") || d.contains("drizzle") || d.contains("shower") ->
            Icons.Filled.WaterDrop to "Rain"
        d.contains("fog") || d.contains("mist") || d.contains("haze") ->
            Icons.Filled.Cloud to "Fog"
        d.contains("wind") -> Icons.Filled.Air to "Windy"
        d.contains("clear") || d.contains("sun") -> Icons.Filled.WbSunny to "Clear"
        d.contains("cloud") || d.contains("overcast") -> Icons.Filled.Cloud to "Cloudy"
        else -> Icons.Filled.Cloud to d.replaceFirstChar { it.uppercase() }
    }
}
