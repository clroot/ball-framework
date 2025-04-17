import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}


allprojects {
    group = "io.clroot"
    version = "2.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        // Kotlin
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib")

        // Arrow-kt
        implementation("io.arrow-kt:arrow-core:2.0.1")

        // Logging
        implementation("org.slf4j:slf4j-api:2.0.9")

        // Testing
        testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
        testImplementation("io.kotest:kotest-assertions-core:5.9.1")
        testImplementation("io.kotest:kotest-assertions-core:5.9.1")
        testImplementation("io.kotest.extensions:kotest-assertions-arrow:2.0.0")
        testImplementation("io.mockk:mockk:1.13.10")
        testImplementation(kotlin("test"))
    }

    task<Jar>("sourcesJar") {
        enabled = true
        archiveClassifier.set("sources")
        from(sourceSets.getByName("main").allSource)
    }

    tasks {
        test {
            useJUnitPlatform()
        }
        compileKotlin {
            compilerOptions {
                freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers", "-opt-in=kotlin.ExperimentalValueClassApi")
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }
}