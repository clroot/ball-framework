dependencies {
    api(project(":domain"))
    api("org.springframework.boot:spring-boot-starter")
    api(project(":shared:arrow"))
    api(project(":shared:lock"))

    testApi("org.springframework.boot:spring-boot-starter-test")
    testApi("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testApi("com.ninja-squad:springmockk:4.0.2")
}
