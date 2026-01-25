package com.hooked.catches.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hooked.core.animation.AnimationConstants

@Composable
fun MapColumn(
    latitude: Double?,
    longitude: Double?,
    location: String?,
    dateCaught: String?,
    translationY: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AnimationConstants.CONTENT_PADDING_DP.dp)
    ) {
        if (latitude != null && longitude != null) {
            StaticMapCard(
                latitude = latitude,
                longitude = longitude,
                modifier = Modifier.aspectRatio(1f)
            )
        }

        if (!location.isNullOrBlank()) {
            AnimatedDetailCard(
                label = "Location",
                value = location,
                translationY = translationY
            )
        }

        if (!dateCaught.isNullOrBlank()) {
            AnimatedDetailCard(
                label = "Date",
                value = dateCaught,
                translationY = translationY
            )
        }
    }
}
