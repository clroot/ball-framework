plugins {
    kotlin("plugin.spring")
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
    testImplementation("org.testcontainers:testcontainers:1.20.6")
    testImplementation("org.testcontainers:junit-jupiter:1.20.6")
    testImplementation("org.testcontainers:redis:1.20.6")
}
