plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
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
                implementation(libs.kotlinx.coroutines.core)
                api(libs.ktor.client.core)
                api(libs.ktor.client.contentnegotiation)
                api(libs.ktor.serialization.kotlinx.json)
                api(libs.koin.core)
            }
        }
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