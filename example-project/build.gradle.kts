group = "io.clroot.example-project"

plugins {
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":ball-user"))
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
}
