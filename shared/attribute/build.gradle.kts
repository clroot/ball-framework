dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:arrow"))
    
    // Jackson (for serialization)
    compileOnly("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    compileOnly("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
}
