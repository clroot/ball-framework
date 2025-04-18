plugins {
    kotlin("plugin.spring")
}

dependencies {
    api(project(":domain"))
    api(project(":application"))
    api(project(":shared:core"))
    api(project(":shared:arrow"))
    api(project(":shared:attribute"))
    api(project(":shared:lock"))

    // Spring Data Redis
    api("org.springframework.boot:spring-boot-starter-data-redis")

    // Spring Integration Redis (for distributed locks)
    implementation("org.springframework.integration:spring-integration-redis")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Lettuce (Redis client)
    implementation("io.lettuce:lettuce-core")
}
