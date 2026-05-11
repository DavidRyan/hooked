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
import androidx.compose.foundation.layout.Row
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

private const val THUMB_SIZE_DP = 64
private const val ROW_PADDING_HORIZONTAL_DP = 16
private const val ROW_PADDING_VERTICAL_DP = 10

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun SharedTransitionScope.TimelineCatchRow(
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
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "row_press_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(
                horizontal = ROW_PADDING_HORIZONTAL_DP.dp,
                vertical = ROW_PADDING_VERTICAL_DP.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(THUMB_SIZE_DP.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .sharedBounds(
                    rememberSharedContentState(key = "catch-image-${catch.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ -> AnimationSpecs.boundsTransformSpring }
                )
        ) {
            AsyncImage(
                imageUrl = catch.imageUrl,
                modifier = Modifier
                    .size(THUMB_SIZE_DP.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = catch.name?.takeIf { it.isNotBlank() } ?: "Unidentified",
                style = MaterialTheme.typography.headlineSmall,
                color = Colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatRowSubtitle(catch.location, catch.dateCaught),
                style = MaterialTheme.typography.bodyMedium,
                color = Colors.subtext1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (catch.enrichmentStatus == EnrichmentStatus.Pending) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Colors.primary)
            )
        }
    }
}

private fun formatRowSubtitle(location: String?, dateCaught: String?): String {
    val time = formatTimeOfDay(dateCaught)
    val loc = location?.takeIf { it.isNotBlank() }
    return when {
        loc != null && time != null -> "$loc · $time"
        loc != null -> loc
        time != null -> time
        else -> "Unknown location"
    }
}
