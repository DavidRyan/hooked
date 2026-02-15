@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hooked.core.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitViewController
import platform.UIKit.UIViewController

/**
 * Native map picker for iOS using MapboxMaps via Swift Package Manager.
 *
 * The actual Mapbox implementation is provided by Swift code in the iOS app.
 * Swift registers a MapViewFactory at app startup that creates the map view controllers.
 */
@Composable
actual fun NativeMapPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (Double, Double) -> Unit,
    modifier: Modifier
) {
    if (MapViewProvider.isRegistered()) {
        // Use the Swift-provided map view
        val mapViewController = remember(latitude, longitude) {
            MapViewProvider.createMapViewController(
                initialLatitude = latitude,
                initialLongitude = longitude,
                onLocationSelected = onLocationSelected
            )
        }

        if (mapViewController != null) {
            UIKitViewController(
                factory = { mapViewController },
                modifier = modifier.fillMaxSize()
            )
        } else {
            FallbackMapView(latitude, longitude, onLocationSelected, modifier)
        }
    } else {
        // Fallback when no map provider is registered
        FallbackMapView(latitude, longitude, onLocationSelected, modifier)
    }
}

@Composable
private fun FallbackMapView(
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (Double, Double) -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.LightGray)
            .clickable {
                val lat = latitude ?: 37.7749
                val lon = longitude ?: -122.4194
                onLocationSelected(lat, lon)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (latitude != null && longitude != null) {
                "Location: ${latitude.toString().take(8)}, ${longitude.toString().take(9)}\nTap to select"
            } else {
                "Map loading...\nTap to select default location"
            },
            color = Color.DarkGray
        )
    }
}
