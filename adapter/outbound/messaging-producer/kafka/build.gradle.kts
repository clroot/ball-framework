plugins {
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    api(project(":adapter:outbound:messaging-producer:core"))

    // Spring Kafka
    implementation("org.springframework.kafka:spring-kafka")
}