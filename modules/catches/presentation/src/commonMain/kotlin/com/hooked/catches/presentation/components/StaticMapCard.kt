package com.hooked.catches.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hooked.core.animation.AnimationConstants
import com.hooked.core.components.AsyncImage
import com.hooked.core.config.AppConfig

@Composable
fun StaticMapCard(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(
            defaultElevation = AnimationConstants.CARD_ELEVATION_DP.dp
        ),
        shape = RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Check if coordinates are valid
            if (latitude != 0.0 && longitude != 0.0 && 
                latitude >= -90 && latitude <= 90 && 
                longitude >= -180 && longitude <= 180) {
                
                // Mapbox Static Images API
                val zoom = 14
                val width = 400
                val height = 400
                val markerColor = "ef4444" // Red color for the pin
                
                // Create Mapbox static map URL with custom marker
                val staticMapUrl = buildString {
                    append("https://api.mapbox.com/styles/v1/mapbox/outdoors-v12/static/")
                    // Add custom marker (pin)
                    append("pin-l+$markerColor($longitude,$latitude)/")
                    // Center coordinates and zoom
                    append("$longitude,$latitude,$zoom/")
                    // Image size
                    append("${width}x$height@2x")
                    // Access token
                    append("?access_token=${AppConfig.MAPBOX_ACCESS_TOKEN}")
                    // Additional parameters
                    append("&attribution=false")
                    append("&logo=false")
                }
                
                AsyncImage(
                    imageUrl = staticMapUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp))
                )
            } else {
                // Show placeholder when no valid location
                Text(
                    text = "Location not available",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}