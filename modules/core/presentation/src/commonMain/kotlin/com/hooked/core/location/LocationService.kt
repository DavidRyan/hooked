package com.hooked.core.location

sealed class LocationResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationResult()
    data class Error(val message: String) : LocationResult()
}

expect class LocationService {
    suspend fun getCurrentLocation(): LocationResult
    fun hasLocationPermission(): Boolean
    suspend fun requestLocationPermission()
}
