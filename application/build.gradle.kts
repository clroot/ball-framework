dependencies {
    api(project(":domain"))
    api("io.arrow-kt:arrow-core:2.0.1")
    implementation(project(":shared:arrow"))
    implementation(project(":shared:lock"))

    compileOnly("org.springframework:spring-context")

    testApi("io.kotest.extensions:kotest-assertions-arrow:2.0.0")
}
