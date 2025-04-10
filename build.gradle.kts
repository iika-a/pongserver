plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}

// Optional: Set the main class for running the app via Gradle
application {
    mainClass.set("pink.iika.pong.MainKt") // Replace with your actual main class
}
