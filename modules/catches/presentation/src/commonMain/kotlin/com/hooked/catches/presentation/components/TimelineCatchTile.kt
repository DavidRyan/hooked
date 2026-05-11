@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.hooked.catches.presentation.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hooked.catches.domain.entities.EnrichmentStatus
import com.hooked.catches.presentation.model.CatchModel
import com.hooked.core.animation.AnimationSpecs
import com.hooked.core.components.AsyncImage
import com.hooked.theme.Colors

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun SharedTransitionScope.TimelineCatchTile(
    catch: CatchModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tile_press"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(Colors.surface1)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        AsyncImage(
            imageUrl = catch.imageUrl,
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    rememberSharedContentState(key = "catch-image-${catch.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> AnimationSpecs.boundsTransformSpring }
                )
        )

        // Bottom gradient for pill legibility on bright images.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )

        // Bottom info pill (species + time)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = catch.name?.takeIf { it.isNotBlank() } ?: "Unidentified",
                style = MaterialTheme.typography.titleSmall,
                color = Colors.white,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = tileSubtitle(catch)
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Colors.white.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Enrichment dot in the top-right corner
        if (catch.enrichmentStatus == EnrichmentStatus.Pending) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Colors.primary)
            )
        }
    }
}

private fun tileSubtitle(catch: CatchModel): String {
    val time = formatTimeOfDay(catch.dateCaught)
    val loc = catch.location?.takeIf { it.isNotBlank() }
    return when {
        loc != null && time != null -> "$loc · $time"
        loc != null -> loc
        time != null -> time
        else -> ""
    }
}
