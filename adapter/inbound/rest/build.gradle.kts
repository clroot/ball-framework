plugins {
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    api(project(":domain"))
    api(project(":application"))
    api(project(":shared:arrow"))

    // Spring Web
    api("org.springframework.boot:spring-boot-starter-web")

    // Validation
    implementation("jakarta.validation:jakarta.validation-api")

    // AOP
    implementation("org.springframework.boot:spring-boot-starter-aop")
}
