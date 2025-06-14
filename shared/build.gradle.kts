plugins {
    kotlin("multiplatform") version "2.1.0"
    id("com.android.library")
    kotlin("plugin.serialization") version "2.1.0"
    id("app.cash.sqldelight") version "2.0.0"
}

android {
    namespace = "com.hooked.shared"

    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                api("io.ktor:ktor-client-core:2.3.10")
                api("io.ktor:ktor-client-content-negotiation:2.3.10")
                api("io.ktor:ktor-serialization-kotlinx-json:2.3.10")
                implementation("app.cash.sqldelight:runtime:2.0.0")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.0")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:android-driver:2.0.0")
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.0.0")
            }
        }
    }
}

sqldelight {
    databases {
        create("HookedDatabase") {
            packageName.set("com.hooked.database")
        }
    }
}