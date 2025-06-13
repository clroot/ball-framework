dependencies {
    api(project(":domain"))
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework:spring-tx")
    api(project(":shared:arrow"))
    api(project(":shared:lock"))

    testApi("org.springframework.boot:spring-boot-starter-test")
    testApi("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testApi("com.ninja-squad:springmockk:4.0.2")
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
