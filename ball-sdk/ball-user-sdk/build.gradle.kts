subprojects {
    dependencies {
        api(project(":domain"))
        api(project(":application"))
        api(project(":adapter:inbound:rest"))
        api(project(":adapter:outbound:data-access-jpa"))
    }
}