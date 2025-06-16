pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "hooked"

include(":shared")
include(":composeApp")

// Legacy feature modules (to be removed)
include(":features:core")
include(":features:catches")
include(":features:submit")

// New modular architecture
// Core modules
include(":modules:core:domain")
include(":modules:core:presentation")

// Shared infrastructure
include(":modules:shared:network")
include(":modules:shared:database")
include(":modules:shared:common")

// Catches feature modules
include(":modules:catches:domain")
include(":modules:catches:data")
include(":modules:catches:presentation")

// Submit feature modules
include(":modules:submit:domain")
include(":modules:submit:data")
include(":modules:submit:presentation")