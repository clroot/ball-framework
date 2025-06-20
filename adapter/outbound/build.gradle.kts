subprojects {
    group = "io.clroot.ball.adapter.outbound"
    dependencies {
        api(project(":domain"))
        api(project(":application"))
        api(project(":shared:arrow"))
        api(project(":shared:jackson"))
    }
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                artifact(tasks["sourcesJar"])
                groupId = this.groupId
                artifactId = this.artifactId
                version = rootProject.version.toString()
            }
        }
    }
}
