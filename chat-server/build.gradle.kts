plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

repositories { mavenCentral() }

val ktorVersion = "3.1.1"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")

    implementation("io.modelcontextprotocol:kotlin-sdk:0.8.3")

    implementation("com.openai:openai-java:2.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

application { mainClass.set("com.hooked.chat.MainKt") }

tasks.shadowJar {
    archiveBaseName.set("hooked-chat-server")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
}

kotlin { jvmToolchain(17) }
