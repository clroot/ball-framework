dependencies {
    api(project(":domain"))
    implementation(project(":shared:arrow"))
    implementation(project(":shared:lock"))

    compileOnly("org.springframework:spring-context")
    compileOnly("org.springframework:spring-tx")
}
