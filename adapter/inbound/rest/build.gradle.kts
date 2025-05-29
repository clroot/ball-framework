plugins {
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    api(project(":domain"))
    api(project(":application"))
    api(project(":shared:arrow"))

    // Spring Web
    api("org.springframework.boot:spring-boot-starter-web")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Validation
    implementation("jakarta.validation:jakarta.validation-api")

    // AOP
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}
