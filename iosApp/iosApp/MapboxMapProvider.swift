import UIKit
import SwiftUI
import ComposeApp
import MapboxMaps

/// Swift implementation of MapViewFactory that provides Mapbox map views to Kotlin.
class MapboxMapFactory: MapViewFactory {
    func createMapViewController(
        initialLatitude: KotlinDouble?,
        initialLongitude: KotlinDouble?,
        onLocationSelected: @escaping (KotlinDouble, KotlinDouble) -> Void
    ) -> UIViewController {
        let lat = initialLatitude?.doubleValue ?? 37.7749
        let lon = initialLongitude?.doubleValue ?? -122.4194

        return MapboxMapViewController(
            initialLatitude: lat,
            initialLongitude: lon,
            onLocationSelected: { latitude, longitude in
                onLocationSelected(KotlinDouble(value: latitude), KotlinDouble(value: longitude))
            }
        )
    }
}

/// UIViewController that displays a Mapbox map with location picking capability.
class MapboxMapViewController: UIViewController {
    private var mapView: MapView!
    private var pointAnnotationManager: PointAnnotationManager?
    private let initialLatitude: Double
    private let initialLongitude: Double
    private let onLocationSelected: (Double, Double) -> Void

    init(initialLatitude: Double, initialLongitude: Double, onLocationSelected: @escaping (Double, Double) -> Void) {
        self.initialLatitude = initialLatitude
        self.initialLongitude = initialLongitude
        self.onLocationSelected = onLocationSelected
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Initialize MapView with the access token from environment
        let mapInitOptions = MapInitOptions(
            cameraOptions: CameraOptions(
                center: CLLocationCoordinate2D(latitude: initialLatitude, longitude: initialLongitude),
                zoom: 12.5
            ),
            styleURI: .outdoors
        )

        mapView = MapView(frame: view.bounds, mapInitOptions: mapInitOptions)
        mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(mapView)

        // Setup annotation manager for markers
        pointAnnotationManager = mapView.annotations.makePointAnnotationManager()

        // Add initial marker if coordinates provided
        addMarker(at: CLLocationCoordinate2D(latitude: initialLatitude, longitude: initialLongitude))

        // Setup tap gesture for location selection
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleMapTap(_:)))
        mapView.addGestureRecognizer(tapGesture)
    }

    @objc private func handleMapTap(_ gesture: UITapGestureRecognizer) {
        let point = gesture.location(in: mapView)
        let coordinate = mapView.mapboxMap.coordinate(for: point)

        // Update marker
        addMarker(at: coordinate)

        // Notify Kotlin
        onLocationSelected(coordinate.latitude, coordinate.longitude)
    }

    private func addMarker(at coordinate: CLLocationCoordinate2D) {
        // Remove existing annotations
        pointAnnotationManager?.annotations.removeAll()

        // Create new marker
        var annotation = PointAnnotation(coordinate: coordinate)
        annotation.iconImage = "mapbox-location-pin"
        annotation.iconSize = 1.5

        // Add to manager
        pointAnnotationManager?.annotations = [annotation]
    }
}

/// Call this function at app startup to register the map provider with Kotlin.
func registerMapboxMapProvider() {
    MapViewProvider.shared.register(factory: MapboxMapFactory())
}
