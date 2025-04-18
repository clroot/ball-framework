subprojects {
    dependencies {
        api(project(":domain"))
        api(project(":application"))
        api(project(":shared:core"))
        api(project(":shared:arrow"))
        api(project(":shared:attribute"))

        // Jackson
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    }
}