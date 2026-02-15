package com.hooked.core.map

import platform.UIKit.UIViewController

/**
 * Factory interface for providing native map views.
 * Swift code registers an implementation that provides Mapbox map views.
 */
object MapViewProvider {
    private var factory: MapViewFactory? = null

    /**
     * Register the map view factory. Called from Swift at app startup.
     */
    fun register(factory: MapViewFactory) {
        this.factory = factory
    }

    /**
     * Create a map view controller with the given configuration.
     */
    fun createMapViewController(
        initialLatitude: Double?,
        initialLongitude: Double?,
        onLocationSelected: (Double, Double) -> Unit
    ): UIViewController? {
        return factory?.createMapViewController(
            initialLatitude = initialLatitude,
            initialLongitude = initialLongitude,
            onLocationSelected = onLocationSelected
        )
    }

    /**
     * Check if a map factory has been registered.
     */
    fun isRegistered(): Boolean = factory != null
}

/**
 * Interface that Swift implements to provide map views.
 */
interface MapViewFactory {
    fun createMapViewController(
        initialLatitude: Double?,
        initialLongitude: Double?,
        onLocationSelected: (Double, Double) -> Unit
    ): UIViewController
}
