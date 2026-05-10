plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "com.hooked.mcp"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:0.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.61.0")
    implementation("org.jetbrains.exposed:exposed-json:0.61.0")

    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

application {
    mainClass.set("com.hooked.mcp.MainKt")
}

tasks.shadowJar {
    archiveBaseName.set("hooked-mcp-server")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "com.hooked.mcp.MainKt"
    }
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

kotlin {
    jvmToolchain(17)
}
