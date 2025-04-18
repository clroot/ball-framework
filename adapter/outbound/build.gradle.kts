subprojects {
    dependencies {
        api(project(":domain"))
        api(project(":application"))
        api(project(":shared:core"))
        api(project(":shared:arrow"))
    }
}