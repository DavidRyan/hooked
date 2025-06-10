plugins {
    kotlin("multiplatform") version "1.9.0" apply false
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.compose") version "1.6.10" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
