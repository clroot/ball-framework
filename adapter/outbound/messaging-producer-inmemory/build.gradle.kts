plugins {
    id("org.jetbrains.kotlin.plugin.spring")
    kotlin("kapt")
}

dependencies {
    // Project dependencies
    api(project(":application"))
    implementation(project(":domain"))
    implementation(project(":shared:core"))
    
    // 공통 메시징 모듈 의존성 (DomainEventWrapper 사용)
    implementation(project(":adapter:shared:messaging-common"))
    
    // Spring dependencies
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.retry:spring-retry")
    
    // Configuration processor
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    
    // Logging
    implementation("org.slf4j:slf4j-api")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.awaitility:awaitility-kotlin")
}
