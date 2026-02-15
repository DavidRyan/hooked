plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
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
                implementation(project(":modules:core:domain"))

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                // Coil for KMP
                implementation("io.coil-kt.coil3:coil-compose:3.0.0-alpha05")
                implementation("io.coil-kt.coil3:coil-network-ktor:3.0.0-alpha05")

                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.ui)
                implementation("androidx.compose.material3:material3:1.3.2")
                implementation("androidx.activity:activity-compose:1.9.3")
                implementation(libs.koin.android)
                implementation(libs.koin.androidx.compose)
                implementation("androidx.exifinterface:exifinterface:1.3.7")

                // Location services
                implementation("com.google.android.gms:play-services-location:21.3.0")

                // Mapbox Maps SDK (Android) — plugins are bundled in the core artifact
                implementation("com.mapbox.maps:android:10.16.6")

                // Coil Android-specific dependencies
                implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0-alpha05")
            }
        }

        val iosX64Main by getting {
            dependencies {
                implementation("io.coil-kt.coil3:coil-network-ktor:3.0.0-alpha05")
            }
        }
        val iosArm64Main by getting {
            dependencies {
                implementation("io.coil-kt.coil3:coil-network-ktor:3.0.0-alpha05")
            }
        }
        val iosSimulatorArm64Main by getting {
            dependencies {
                implementation("io.coil-kt.coil3:coil-network-ktor:3.0.0-alpha05")
            }
        }

    }
}

android {
    namespace = "com.hooked.core.presentation"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
