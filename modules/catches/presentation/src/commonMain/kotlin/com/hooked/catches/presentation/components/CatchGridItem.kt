package com.hooked.catches.presentation.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.hooked.core.animation.AnimationSpecs
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
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
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
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
    val background = HookedTheme.surface.copy(alpha = 0.92f)
    val borderColor = HookedTheme.tertiary.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp)
            .size(30.dp)
            .clip(CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            EnrichmentStatus.Pending -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = HookedTheme.primary
                )
            }

            EnrichmentStatus.Completed -> {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Enrichment complete",
                    tint = HookedTheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            EnrichmentStatus.Failed -> {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = "Enrichment failed",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
