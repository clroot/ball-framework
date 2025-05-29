plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    // Core dependencies
    api(project(":application"))
    api(project(":domain"))

    // Logging
    implementation("org.slf4j:slf4j-api")

    // Test dependencies
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.mockk:mockk")
}

description = "Ball Framework - Event Publisher Core"
