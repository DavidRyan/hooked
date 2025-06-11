plugins {
    kotlin("multiplatform") version "2.1.0"
    id("com.android.library")
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:1.6.10")
                implementation("org.jetbrains.compose.foundation:foundation:1.6.10")
                implementation("org.jetbrains.compose.material3:material3:1.6.10")
            }        }
        val androidMain by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
        }
    }
}
dependencies {
    // Add this to enable the Compose dependency helper
}
android {
    namespace = "com.hooked.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}