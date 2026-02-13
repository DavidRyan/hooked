package com.hooked.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import cocoapods.MapboxMaps.MapInitOptions
import cocoapods.MapboxMaps.MapView
import cocoapods.MapboxMaps.PointAnnotationManager
import cocoapods.MapboxMaps.PointAnnotationOptions
import cocoapods.MapboxMaps.ResourceOptions
import cocoapods.MapboxMaps.Style
import cocoapods.Turf.Point
import com.hooked.core.config.AppConfig
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeMapPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (Double, Double) -> Unit,
    modifier: Modifier
) {
    val mapView = remember {
        val resourceOptions = ResourceOptions.Builder()
            .accessToken(AppConfig.MAPBOX_ACCESS_TOKEN)
            .build()
        val mapInitOptions = MapInitOptions(resourceOptions = resourceOptions)
        MapView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), mapInitOptions = mapInitOptions)
    }

    val annotationManager = remember {
        mapView.annotations.makePointAnnotationManager()
    }

    fun updateMarker(lat: Double, lon: Double) {
        annotationManager.deleteAll()
        val point = Point.fromLngLat(lon, lat)
        val options = PointAnnotationOptions().withPoint(point)
        annotationManager.create(options)
    }

    LaunchedEffect(Unit) {
        mapView.mapboxMap.loadStyleURI(Style.MAPBOX_STREETS)
        mapView.gestures.onMapTap = { screenPoint ->
            val coordinate = mapView.mapboxMap.coordinateForPoint(screenPoint)
            val lat = coordinate.latitude
            val lon = coordinate.longitude
            updateMarker(lat, lon)
            onLocationSelected(lat, lon)
            true
        }
    }

    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null) {
            updateMarker(latitude, longitude)
            mapView.mapboxMap.setCamera(
                cocoapods.MapboxMaps.CameraOptions.Builder()
                    .center(Point.fromLngLat(longitude, latitude))
                    .zoom(12.5)
                    .build()
            )
        }
    }

    UIKitView(
        modifier = modifier,
        factory = { mapView },
        update = { }
    )
}
