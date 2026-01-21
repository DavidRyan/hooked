import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
}

// Load .env file from project root
val envFile = rootProject.file(".env")
val envProperties = Properties().apply {
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
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
                // Coil for KMP
                implementation("io.coil-kt.coil3:coil-compose:3.0.0-alpha05")
                implementation("io.coil-kt.coil3:coil-network-ktor:3.0.0-alpha05")
                implementation(libs.runtime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.0-beta01")
                implementation(libs.kotlinx.serialization.json)
                implementation(project(":modules:core:domain"))
                implementation(project(":modules:core:presentation"))
                implementation(project(":modules:catches:presentation"))
                implementation(project(":modules:catches:domain"))
                implementation(project(":modules:catches:data"))
                implementation(project(":modules:auth:presentation"))
                implementation(project(":modules:auth:domain"))
                implementation(project(":modules:auth:data"))
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)

            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.ui)
                implementation("androidx.compose.material3:material3:1.3.2")
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.android)
                implementation(libs.koin.androidx.compose)
                implementation("androidx.exifinterface:exifinterface:1.3.7")
                implementation(libs.ktor.client.android)
                // Coil Android-specific dependencies
                implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0-alpha05")


            }
        }

        val iosX64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
                // Coil iOS-specific dependencies
                implementation("io.coil-kt.coil3:coil-network-ktor:3.0.0-alpha05")
            }
        }
        val iosArm64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
                // Coil iOS-specific dependencies
                implementation("io.coil-kt.coil3:coil-network-ktor:3.0.0-alpha05")
            }
        }
        val iosSimulatorArm64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
                // Coil iOS-specific dependencies
                implementation("io.coil-kt.coil3:coil-network-ktor:3.0.0-alpha05")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.koin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        
        val androidUnitTest by getting {
            dependencies {
                implementation("io.mockk:mockk:1.13.12")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}

android {

    namespace = "com.hooked.ui"
    compileSdk = 36
    defaultConfig {
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.hooked"
        minSdk = 24
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
        
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN",
            envProperties.getProperty("MAPBOX_ACCESS_TOKEN", "")
        )
        buildConfigField("String", "API_BASE_URL", "\"${envProperties.getProperty("API_BASE_URL", "http://10.0.2.2:4000/api")}\"")
    }
    
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}

android {
}