plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    // Core dependencies
    api(project(":application"))
    api(project(":domain"))
    api(project(":shared:core"))

    // Spring dependencies
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework:spring-context")
    api("org.springframework:spring-tx")

    // Coroutines for async processing
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Logging
    implementation("org.slf4j:slf4j-api")

    // Test dependencies
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.mockk:mockk")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.awaitility:awaitility-kotlin")
}

description = "Ball Framework - Event Consumer Core"
