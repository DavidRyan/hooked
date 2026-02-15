import org.gradle.authentication.http.BasicAuthentication
import java.util.Properties

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven {
            val envFile = rootDir.resolve(".env")
            val envProperties = Properties().apply {
                if (envFile.exists()) {
                    envFile.inputStream().use { load(it) }
                }
            }
            val token = (providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").orNull
                ?: System.getenv("MAPBOX_DOWNLOADS_TOKEN")
                ?: envProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN")
                ?: "").trim().removeSurrounding("\"")
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = token
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

rootProject.name = "hooked"

include(":composeApp")
include(":modules:core:domain")
include(":modules:core:presentation")
include(":modules:catches:data")
include(":modules:catches:domain")
include(":modules:catches:presentation")
include(":modules:skunks:data")
include(":modules:skunks:domain")
include(":modules:skunks:presentation")
include(":modules:auth:data")
include(":modules:auth:domain")
include(":modules:auth:presentation")
