package com.hooked.catches.presentation.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hooked.core.animation.AnimationSpecs
import com.hooked.core.components.AsyncImage
import com.hooked.theme.Colors

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.CatchHero(
    catchId: String,
    photoUrl: String?,
    species: String?,
    location: String?,
    weatherDescription: String? = null,
    tempFahrenheit: Float? = null,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
    ) {
        AsyncImage(
            imageUrl = photoUrl ?: "",
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    rememberSharedContentState(key = "catch-image-${catchId}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> AnimationSpecs.boundsTransformSpring }
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        WeatherBadge(
            description = weatherDescription,
            tempFahrenheit = tempFahrenheit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 16.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = species?.takeIf { it.isNotBlank() } ?: "Unidentified",
                style = MaterialTheme.typography.displayMedium,
                color = Colors.white,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            location?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Colors.white.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
