package com.hooked.catches.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hooked.theme.Colors
import kotlin.math.roundToInt

private data class Stat(val value: String, val label: String)

@Composable
fun StatStrip(
    weatherData: Map<String, String?>?,
    modifier: Modifier = Modifier
) {
    val stats = buildStats(weatherData)
    if (stats.isEmpty()) return

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(items = stats) { stat ->
            StatChip(value = stat.value, label = stat.label)
            if (stat != stats.last()) {
                VerticalDivider(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(vertical = 8.dp),
                    color = Colors.overlay0
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = Colors.text
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Colors.subtext0
        )
    }
}

private fun buildStats(weather: Map<String, String?>?): List<Stat> {
    if (weather == null) return emptyList()

    fun pickFloat(vararg keys: String): Float? = keys.firstNotNullOfOrNull { weather[it]?.toFloatOrNull() }

    return buildList {
        pickFloat("temp", "temperature")?.let { add(Stat("${it.roundToInt()}°", "Air")) }
        pickFloat("water_temp")?.let { add(Stat("${it.roundToInt()}°", "Water")) }
        pickFloat("wind_speed")?.let { add(Stat(it.roundToInt().toString(), "Wind")) }
        pickFloat("humidity")?.let { add(Stat("${it.roundToInt()}%", "Humidity")) }
        pickFloat("pressure")?.let { add(Stat(it.roundToInt().toString(), "Pressure")) }
    }
}
