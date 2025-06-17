package com.hooked.catches.presentation.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.hooked.core.animation.AnimationSpecs
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hooked.catches.presentation.model.CatchModel
import com.hooked.core.animation.AnimationConstants
import com.hooked.core.components.AsyncImage
import com.hooked.theme.HookedTheme

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.CatchGridItem(
    catch: CatchModel,
    onClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = AnimationConstants.CARD_ELEVATION_DP.dp
        ),
        colors = CardDefaults.cardColors(containerColor = HookedTheme.surface)
    ) {
        AsyncImage(
            imageUrl = catch.imageUrl,
            modifier = Modifier
                .fillMaxSize()
                .sharedElement(
                    rememberSharedContentState(key = "catch-image-${catch.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ ->
                        AnimationSpecs.boundsTransformSpring
                    }
                )
                .clip(RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp))
        )
    }
}