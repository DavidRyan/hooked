package com.hooked.catches.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hooked.theme.Colors

@Composable
fun MapCard(
    latitude: Double?,
    longitude: Double?,
    location: String?,
    modifier: Modifier = Modifier
) {
    if (latitude == null || longitude == null) return

    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        StaticMapCard(
            latitude = latitude,
            longitude = longitude,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        location?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = Colors.subtext1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
