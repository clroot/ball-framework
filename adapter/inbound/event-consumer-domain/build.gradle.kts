plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    // Core dependencies
    api(project(":adapter:inbound:event-consumer-core"))
    api(project(":adapter:shared:messaging-common"))
    api(project(":application"))
    api(project(":domain"))

    // Spring dependencies
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework:spring-tx") // 트랜잭션 지원

    // Async processing
    implementation("org.springframework:spring-context")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // Metrics and monitoring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-core")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.mockk:mockk")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
}

description = "Ball Framework - Domain Event Consumer"
