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
    api(project(":adapter:outbound:data-access-core"))

    // Spring Data JPA
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // Hibernate
    api("org.hibernate.orm:hibernate-core")
    api("io.hypersistence:hypersistence-utils-hibernate-63:3.10.1")

    // Connection pooling
    api("com.zaxxer:HikariCP")

    // Kotlin JDSL
    api("com.linecorp.kotlin-jdsl:jpql-dsl:3.5.5")
    api("com.linecorp.kotlin-jdsl:jpql-render:3.5.5")
    api("com.linecorp.kotlin-jdsl:spring-data-jpa-support:3.5.5")
}
