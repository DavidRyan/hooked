package com.hooked.catches.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.hooked.core.animation.AnimationSpecs
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import com.hooked.catches.presentation.model.CatchModel
import com.hooked.catches.domain.entities.EnrichmentStatus
import com.hooked.core.animation.AnimationConstants
import com.hooked.core.components.AsyncImage
import com.hooked.theme.HookedTheme

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun SharedTransitionScope.CatchGridItem(
    catch: CatchModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    index: Int = 0,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press_scale"
    )

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(minOf(index, 8) * 55L)
        entered = true
    }
    val entranceScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.88f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "entrance_scale"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "entrance_alpha"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = entranceScale * scale
                scaleY = entranceScale * scale
                alpha = entranceAlpha
            }
            .aspectRatio(1f)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = AnimationConstants.CARD_ELEVATION_DP.dp
        ),
        colors = CardDefaults.cardColors(containerColor = HookedTheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    rememberSharedContentState(key = "catch-image-${catch.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ ->
                        AnimationSpecs.boundsTransformSpring
                    }
                )
                .border(
                    width = AnimationConstants.IMAGE_BORDER_WIDTH_DP.dp,
                    color = HookedTheme.tertiary,
                    shape = RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp)
                )
                .clip(RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp))
        ) {
            AsyncImage(
                imageUrl = catch.imageUrl,
                modifier = Modifier.fillMaxSize()
            )

            EnrichmentStatusBadge(status = catch.enrichmentStatus)
        }
    }
}

@Composable
private fun BoxScope.EnrichmentStatusBadge(status: EnrichmentStatus) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(status) {
        when (status) {
            EnrichmentStatus.Pending -> visible = true
            EnrichmentStatus.Completed, EnrichmentStatus.Failed -> {
                visible = true
                delay(3_000L)
                visible = false
            }
        }
    }

    // One-shot shimmer on Completed
    val shimmerProgress = remember { Animatable(0f) }
    LaunchedEffect(status) {
        if (status == EnrichmentStatus.Completed) {
            shimmerProgress.snapTo(0f)
            shimmerProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = LinearEasing)
            )
        }
    }
    val shimmerValue = shimmerProgress.value

    // Spring-punch scale for icon on Completed/Failed
    val iconScale = remember { Animatable(0f) }
    LaunchedEffect(status) {
        if (status != EnrichmentStatus.Pending) {
            iconScale.snapTo(0f)
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val background   = HookedTheme.surface.copy(alpha = 0.92f)
    val borderColor  = HookedTheme.tertiary.copy(alpha = 0.6f)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(600)),
        modifier = Modifier.align(Alignment.TopEnd)
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(background, CircleShape)
                .drawBehind {
                    val r = size.minDimension / 2f
                    // Border
                    drawCircle(
                        color = borderColor,
                        radius = r,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    // Shimmer sweep
                    if (shimmerValue > 0f) {
                        val sweepWidth = size.width * 0.75f
                        val centerX = shimmerValue * (size.width + sweepWidth) - sweepWidth / 2f
                        drawCircle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                startX = centerX - sweepWidth / 2f,
                                endX   = centerX + sweepWidth / 2f
                            ),
                            radius = r
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                EnrichmentStatus.Pending -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = HookedTheme.primary
                )
                EnrichmentStatus.Completed -> Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Enrichment complete",
                    tint = HookedTheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value }
                )
                EnrichmentStatus.Failed -> Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Enrichment failed",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value }
                )
            }
        }
    }
}
