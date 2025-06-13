dependencies {
    testImplementation("com.aallam.ulid:ulid-kotlin:1.3.0")
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
