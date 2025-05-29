plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    // Core dependencies
    api(project(":adapter:outbound:event-publisher-core"))
    api(project(":adapter:shared:messaging-common"))
    api(project(":application"))
    api(project(":domain"))

    // Spring dependencies
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-configuration-processor")

    // Metrics and monitoring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-core")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.mockk:mockk")
}

description = "Ball Framework - Domain Event Publisher"
