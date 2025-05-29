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
    api(project(":domain"))
    api(project(":application"))
    api(project(":shared:core"))
    api(project(":shared:attribute"))
    api(project(":adapter:inbound:rest"))
    api(project(":adapter:outbound:persistence-jpa"))
}
