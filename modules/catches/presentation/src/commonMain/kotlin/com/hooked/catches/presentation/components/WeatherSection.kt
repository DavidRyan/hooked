package com.hooked.catches.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hooked.catches.presentation.model.WeatherUi
import kotlin.math.roundToInt

@Composable
fun WeatherSection(
    weather: WeatherUi,
    modifier: Modifier = Modifier,
    translationY: Float = 0f
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
        ) {
            WindParticles(
                modifier = Modifier.fillMaxSize(),
                windFromDegrees = weather.windDirection,
                windSpeed = weather.windSpeed,
                particleCount = 200,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                speedScale = 24f,
                turbulence = 0.16f
            )
            LabelChip(
                text = "Wind ${weather.windSpeed.roundToInt()} m/s • ${weather.windDirection.roundToInt()}°",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }

        if (weather.weatherText.isNotEmpty()) {
            AnimatedDetailCard(
                label = "Weather",
                value = weather.weatherText,
                translationY = translationY
            )
        }
    }
}
