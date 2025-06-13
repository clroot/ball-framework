subprojects {
    group = "io.clroot.ball.shared"
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
