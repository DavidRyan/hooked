package com.hooked.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.hooked.core.config.AppConfig
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.removeOnMapClickListener

@Composable
actual fun NativeMapPicker(
    latitude: Double?,
    longitude: Double?,
    onLocationSelected: (Double, Double) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            getMapboxMap().loadStyleUri(Style.OUTDOORS)
        }
    }

    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }

    fun updateMarker(lat: Double, lon: Double) {
        pointAnnotationManager?.deleteAll()
        val point = Point.fromLngLat(lon, lat)
        val options = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage("mapbox-location-pin")
        pointAnnotationManager?.create(options)
    }

    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null) {
            updateMarker(latitude, longitude)
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(longitude, latitude))
                    .zoom(12.5)
                    .build()
            )
        }
    }

    val clickListener = remember {
        OnMapClickListener { point ->
            val lat = point.latitude()
            val lon = point.longitude()
            updateMarker(lat, lon)
            onLocationSelected(lat, lon)
            true
        }
    }

    DisposableEffect(mapView) {
        pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        mapView.getMapboxMap().addOnMapClickListener(clickListener)

        onDispose {
            mapView.getMapboxMap().removeOnMapClickListener(clickListener)
            pointAnnotationManager?.deleteAll()
            pointAnnotationManager = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { }
    )
}
