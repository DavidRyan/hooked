import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
}

// Load .env file
val envFile = rootProject.file(".env")
val envProperties = Properties()
if (envFile.exists()) {
    envFile.inputStream().use { envProperties.load(it) }
}

// Get Mapbox token from .env file
val mapboxToken = envProperties.getProperty("MAP_BOX_TOKEN")?.trim('"') ?: "YOUR_MAPBOX_ACCESS_TOKEN"

// Task to generate MapConfig.kt
val generateMapConfig = tasks.register("generateMapConfig") {
    val outputDir = layout.buildDirectory.dir("generated/source/mapconfig/commonMain/kotlin/com/hooked/core/config")
    val outputFile = outputDir.get().file("MapConfig.kt")
    
    outputs.file(outputFile)
    
    doLast {
        outputDir.get().asFile.mkdirs()
        outputFile.asFile.writeText("""
            package com.hooked.core.config
            
            object MapConfig {
                const val MAPBOX_ACCESS_TOKEN = "$mapboxToken"
            }
        """.trimIndent())
    }
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-module-name=core-domain")
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("generated/source/mapconfig/commonMain/kotlin"))
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
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
    namespace = "com.hooked.core.domain"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Make compile tasks depend on generateMapConfig
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateMapConfig)
}

// Make Kotlin metadata compilation tasks depend on generateMapConfig
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateMapConfig)
}