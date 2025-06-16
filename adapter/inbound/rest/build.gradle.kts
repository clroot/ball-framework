plugins {
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    api(project(":domain"))
    api(project(":application"))
    api(project(":shared:arrow"))
    api(project(":shared:jackson"))
    api(project(":adapter:outbound:data-access-core"))

    // Spring Web
    api("org.springframework.boot:spring-boot-starter-web")

    // Validation
    api("jakarta.validation:jakarta.validation-api")

    // AOP
    implementation("org.springframework.boot:spring-boot-starter-aop")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}
