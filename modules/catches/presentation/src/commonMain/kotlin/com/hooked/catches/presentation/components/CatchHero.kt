package com.hooked.catches.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hooked.core.animation.AnimationSpecs
import com.hooked.core.components.AsyncImage
import com.hooked.theme.Colors
import kotlinx.coroutines.delay

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
    // Drive overlay visibility from local state instead of the AnimatedContent
    // scope. The sharedBounds transition seems to make sibling content flicker
    // when relying on scope-driven enter/exit. Local state + AnimatedVisibility
    // gives a clean controlled fade-in regardless of what the scope is doing.
    var overlayVisible by remember(catchId) { mutableStateOf(false) }
    LaunchedEffect(catchId) {
        // Tiny delay so the shared-bounds photo has a moment to start morphing
        // before the title slides up over it. ~60ms is just below perceptual
        // threshold — feels instant but staggers the elements pleasantly.
        delay(60)
        overlayVisible = true
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
    ) {
        // Photo morphs from tile → hero via shared bounds.
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

        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(animationSpec = tween(320)),
            exit = fadeOut(animationSpec = tween(120)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 16.dp)
        ) {
            WeatherBadge(
                description = weatherDescription,
                tempFahrenheit = tempFahrenheit
            )
        }

        AnimatedVisibility(
            visible = overlayVisible,
            enter = fadeIn(animationSpec = tween(360)) +
                slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(360)
                ),
            exit = fadeOut(animationSpec = tween(120)),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Column(
                modifier = Modifier
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
}
