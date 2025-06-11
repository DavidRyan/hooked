plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
}

kotlin {
    jvmToolchain(17)
    ios()
    cocoapods {
        summary = "Hooked – log your legendary catches"
        homepage = "https://hooked.app"
        version = "1.0.0" // ✅ this is what fixes the error
        ios.deploymentTarget = "14.1"
        framework {
            baseName = "shared"
        }
    }
}