plugins {
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    api(project(":domain"))
    api(project(":shared:arrow"))

    api("org.springframework.data:spring-data-commons")
    api("org.springframework:spring-context")
    api("org.springframework:spring-tx")

    // Jackson for JSON processing
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
}
