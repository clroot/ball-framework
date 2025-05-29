plugins {
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("org.jetbrains.kotlin.plugin.allopen")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    api(project(":adapter:outbound:persistence-core"))

    // Spring Data JPA
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // Hibernate
    implementation("org.hibernate.orm:hibernate-core")

    // Connection pooling
    implementation("com.zaxxer:HikariCP")

    // Kotlin JDSL
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:3.3.0")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:3.3.0")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-support:3.3.0")
}
