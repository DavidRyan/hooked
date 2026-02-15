package com.hooked.core.location

import androidx.compose.runtime.Composable
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined

@Composable
actual fun LocationPermissionRequester(
    onPermissionResult: (granted: Boolean) -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
) {
    val locationManager = CLLocationManager()

    content {
        val status = locationManager.authorizationStatus
        if (status == kCLAuthorizationStatusAuthorizedWhenInUse ||
            status == kCLAuthorizationStatusAuthorizedAlways) {
            onPermissionResult(true)
        } else if (status == kCLAuthorizationStatusNotDetermined) {
            locationManager.requestWhenInUseAuthorization()
            // iOS will show the system dialog; re-checking must happen on next interaction
            onPermissionResult(false)
        } else {
            onPermissionResult(false)
        }
    }
}
