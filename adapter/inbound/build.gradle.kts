subprojects {
    group = "io.clroot.ball.adapter.inbound"
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
                println("sibal: ${this.groupId} ${this.artifactId} ${this.version}")
            }
        }
    }
}
