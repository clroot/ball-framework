plugins {
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    api(project(":adapter:outbound:messaging-producer-core"))

    // Spring Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
