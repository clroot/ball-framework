plugins {
    kotlin("plugin.spring")
}

dependencies {
    api(project(":domain"))
    api(project(":application"))
    api(project(":shared:core"))
    api(project(":shared:attribute"))
    api(project(":adapter:inbound:rest"))
    api(project(":adapter:outbound:persistence-jpa"))
}
