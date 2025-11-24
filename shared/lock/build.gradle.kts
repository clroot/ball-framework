plugins {
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    implementation(project(":shared:arrow"))

    // Spring (AOP만 필요, SpEL 제거)
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")

    // Logging
    implementation("org.slf4j:slf4j-api")
}
