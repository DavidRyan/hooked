plugins {
    kotlin("multiplatform") version "2.1.0" apply false
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
