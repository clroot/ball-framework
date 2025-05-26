plugins {
    kotlin("plugin.spring")
}

dependencies {
    // Project dependencies
    api(project(":domain"))
    
    // Spring dependencies
    implementation("org.springframework:spring-context")
    
    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
