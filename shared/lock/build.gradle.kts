dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:arrow"))

    // Spring
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework:spring-expression")
    implementation("org.springframework.boot:spring-boot-starter-aop")
}
