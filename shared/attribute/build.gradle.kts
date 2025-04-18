dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:arrow"))
    
    // Jackson (for serialization)
    compileOnly("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
}
