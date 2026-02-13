package com.hooked.core.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class LocationService(
    private val activity: ComponentActivity
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

    actual suspend fun getCurrentLocation(): LocationResult {
        if (!hasLocationPermission()) {
            return LocationResult.Error("Location permission not granted")
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val cancellationTokenSource = CancellationTokenSource()

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(
                            LocationResult.Success(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        )
                    } else {
                        // Fall back to last known location
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLocation ->
                                if (lastLocation != null) {
                                    continuation.resume(
                                        LocationResult.Success(
                                            latitude = lastLocation.latitude,
                                            longitude = lastLocation.longitude
                                        )
                                    )
                                } else {
                                    continuation.resume(
                                        LocationResult.Error("Unable to determine location. Make sure location services are enabled.")
                                    )
                                }
                            }
                            .addOnFailureListener { e ->
                                continuation.resume(
                                    LocationResult.Error("Failed to get location: ${e.message}")
                                )
                            }
                    }
                }.addOnFailureListener { e ->
                    continuation.resume(
                        LocationResult.Error("Failed to get location: ${e.message}")
                    )
                }
            } catch (e: SecurityException) {
                continuation.resume(
                    LocationResult.Error("Location permission denied: ${e.message}")
                )
            } catch (e: Exception) {
                continuation.resume(
                    LocationResult.Error("Location error: ${e.message}")
                )
            }
        }
    }

    actual fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    actual suspend fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1001
        )
    }
}
