plugins {
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.kotlin.plugin.jpa")
}

dependencies {
    api(project(":shared:attribute"))

    // Spring Data JPA
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // Hibernate
    implementation("org.hibernate.orm:hibernate-core")

    // Connection pooling
    implementation("com.zaxxer:HikariCP")

    // Database drivers
    compileOnly("com.h2database:h2")
    compileOnly("org.postgresql:postgresql")
    compileOnly("com.mysql:mysql-connector-j")
}
