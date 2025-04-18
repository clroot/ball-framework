plugins {
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.kotlin.plugin.jpa")
}

dependencies {
    api(project(":domain"))
    api(project(":application"))
    api(project(":shared:core"))
    api(project(":shared:arrow"))
    api(project(":shared:attribute"))

    // Spring Data JPA
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Hibernate
    implementation("org.hibernate.orm:hibernate-core")

    // Connection pooling
    implementation("com.zaxxer:HikariCP")

    // Database drivers
    compileOnly("com.h2database:h2")
    compileOnly("org.postgresql:postgresql")
    compileOnly("com.mysql:mysql-connector-j")
}
