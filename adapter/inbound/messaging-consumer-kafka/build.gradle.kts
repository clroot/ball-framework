plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "io.clroot.ball"
version = "2.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Project dependencies
    api(project(":adapter:inbound:messaging-consumer-core"))
    implementation(project(":domain"))
    implementation(project(":shared:core"))
    implementation(project(":shared:arrow"))

    // Spring dependencies
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-configuration-processor")

    // Spring Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Jackson for JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Logging
    implementation("org.slf4j:slf4j-api")

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
    testImplementation("io.mockk:mockk")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.awaitility:awaitility-kotlin")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
