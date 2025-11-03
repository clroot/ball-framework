plugins {
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    api(project(":shared:lock"))

    // Spring Data Redis
    api("org.springframework.boot:spring-boot-starter-data-redis")

    // Spring Integration Redis (for distributed locks)
    implementation("org.springframework.integration:spring-integration-redis")

    // Lettuce (Redis client)
    implementation("io.lettuce:lettuce-core")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:2.0.1")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.1")
    testImplementation("com.redis:testcontainers-redis:2.2.4")
}
