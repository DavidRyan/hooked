import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Register the Mapbox map provider with Kotlin
        registerMapboxMapProvider()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
