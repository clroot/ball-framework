subprojects {
    dependencies {
        api(project(":domain"))
        api(project(":application"))
        api(project(":shared:arrow"))
    }
}