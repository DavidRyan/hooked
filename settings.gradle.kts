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

include(":composeApp")
include(":modules:core:domain")
include(":modules:core:presentation")
include(":modules:catches:data")
include(":modules:catches:domain")
include(":modules:catches:presentation")
include(":modules:submit:data")
include(":modules:submit:domain")
include(":modules:submit:presentation")