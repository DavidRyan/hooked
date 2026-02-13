package com.hooked.core.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLLocation
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class LocationService {
    private val locationManager = CLLocationManager()

    actual suspend fun getCurrentLocation(): LocationResult {
        if (!hasLocationPermission()) {
            return LocationResult.Error("Location permission not granted")
        }

        return suspendCancellableCoroutine { continuation ->
            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(
                    manager: CLLocationManager,
                    didUpdateLocations: List<*>
                ) {
                    manager.delegate = null
                    val location = didUpdateLocations.lastOrNull() as? CLLocation
                    if (location != null) {
                        val lat = location.coordinate.useContents { latitude }
                        val lng = location.coordinate.useContents { longitude }
                        continuation.resume(
                            LocationResult.Success(
                                latitude = lat,
                                longitude = lng
                            )
                        )
                    } else {
                        continuation.resume(
                            LocationResult.Error("Unable to determine location")
                        )
                    }
                }

                override fun locationManager(
                    manager: CLLocationManager,
                    didFailWithError: NSError
                ) {
                    manager.delegate = null
                    continuation.resume(
                        LocationResult.Error("Location error: ${didFailWithError.localizedDescription}")
                    )
                }
            }

            locationManager.delegate = delegate
            locationManager.desiredAccuracy = kCLLocationAccuracyBest
            locationManager.requestLocation()

            continuation.invokeOnCancellation {
                locationManager.delegate = null
            }
        }
    }

    actual fun hasLocationPermission(): Boolean {
        val status = CLLocationManager().authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
               status == kCLAuthorizationStatusAuthorizedAlways
    }

    actual suspend fun requestLocationPermission() {
        locationManager.requestWhenInUseAuthorization()
    }
}
