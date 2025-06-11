plugins {
    kotlin("multiplatform") version "2.1.0"
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("media.kamel:kamel-image-default:1.0.5")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.compose.ui:ui:1.8.2")
                implementation("androidx.compose.material3:material3:1.3.2")
                implementation("androidx.activity:activity-compose:1.10.1")
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        val iosMain by creating {
        }
    }
}

android {
    namespace = "com.hooked.ui"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}