plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-module-name=auth-domain")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":modules:core:domain"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val androidMain by getting {
            dependencies {
                
            }
        }

    }
}

android {
    namespace = "com.hooked.auth.domain"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}