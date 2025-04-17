dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:arrow"))

    // Spring (optional)
    compileOnly("org.springframework:spring-context:6.1.3")
    compileOnly("org.springframework:spring-tx:6.1.3")

    // AspectJ (optional)
    compileOnly("org.aspectj:aspectjweaver:1.9.21")

    // Spring Expression Language (SpEL) (optional)
    compileOnly("org.springframework:spring-expression:6.1.3")

    // Test dependencies
    testImplementation("org.springframework:spring-context:6.1.3")
    testImplementation("org.springframework:spring-tx:6.1.3")
    testImplementation("org.aspectj:aspectjweaver:1.9.21")
    testImplementation("org.springframework:spring-expression:6.1.3")
}
