package com.hooked.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.hooked.core.config.AppConfig
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures

@Composable
actual fun NativeMapPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (Double, Double) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val currentOnLocationSelected by rememberUpdatedState(onLocationSelected)
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }

    // Remember initial coordinates to set camera only once
    val initialLat = remember { latitude ?: 37.7749 }
    val initialLon = remember { longitude ?: -122.4194 }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // Set initial camera position in MapInitOptions to avoid animation
            val cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(initialLon, initialLat))
                .zoom(12.5)
                .build()

            val mapInitOptions = MapInitOptions(
                context = ctx,
                resourceOptions = ResourceOptions.Builder()
                    .accessToken(AppConfig.MAPBOX_ACCESS_TOKEN)
                    .build(),
                cameraOptions = cameraOptions
            )

            MapView(ctx, mapInitOptions).apply {
                val map = getMapboxMap()

                // Disable all tap-based zoom gestures
                gestures.doubleTapToZoomInEnabled = false
                gestures.doubleTouchToZoomOutEnabled = false
                gestures.quickZoomEnabled = false

                map.loadStyleUri(Style.OUTDOORS) { style ->
                    // Style loaded - set up annotations
                    val annotationManager = annotations.createPointAnnotationManager()
                    pointAnnotationManager = annotationManager

                    // Add initial marker if coordinates provided
                    if (latitude != null && longitude != null) {
                        val point = Point.fromLngLat(longitude, latitude)
                        val options = PointAnnotationOptions()
                            .withPoint(point)
                            .withIconImage("marker-15")
                        annotationManager.create(options)
                    }

                    // Click listener for marker placement
                    map.addOnMapClickListener { point ->
                        val lat = point.latitude()
                        val lon = point.longitude()

                        // Update marker
                        annotationManager.deleteAll()
                        val options = PointAnnotationOptions()
                            .withPoint(point)
                            .withIconImage("marker-15")
                        annotationManager.create(options)

                        // Notify parent
                        currentOnLocationSelected(lat, lon)
                        true
                    }
                }
            }
        },
        update = { /* No updates needed - camera and markers managed in factory */ }
    )
}
