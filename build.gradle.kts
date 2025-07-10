import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.*

plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0-rc.1"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.1.20"
    id("org.jetbrains.kotlin.plugin.jpa") version "2.1.20"
    id("org.jetbrains.kotlin.plugin.spring") version "2.1.20"
    id("maven-publish")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

allprojects {
    apply {
        plugin("maven-publish")
    }
    group = "io.clroot.ball"
    version = "2.0.0-20250710.2-SNAPSHOT"

    val nexusUsername = localProperties.getProperty("nexus.username")
    val nexusPassword = localProperties.getProperty("nexus.password")

    repositories {
        mavenCentral()
        maven {
            url = uri("https://nexus.clroot.io/repository/maven-snapshots/")
        }
        maven {
            url = uri("https://nexus.clroot.io/repository/maven-releases/")
        }
    }

    publishing {
        repositories {
            maven {
                val releasesRepoUrl = uri("https://nexus.clroot.io/repository/maven-releases/")
                val snapshotsRepoUrl = uri("https://nexus.clroot.io/repository/maven-snapshots/")
                credentials {
                    username = nexusUsername
                    password = nexusPassword
                }
                url = if (rootProject.version.toString().endsWith("RELEASE")) releasesRepoUrl else snapshotsRepoUrl
            }
        }
    }
}

tasks.named("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}

subprojects {
    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.springframework.boot")
        plugin("io.spring.dependency-management")
    }

    tasks.named("bootJar") {
        enabled = false
    }

    tasks.named<Jar>("jar") {
        enabled = true
        archiveClassifier.set("")
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
        api("org.jetbrains.kotlin:kotlin-reflect")
        api("org.jetbrains.kotlin:kotlin-stdlib")

        // Logging
        api("org.slf4j:slf4j-api")

        // Testing
        testApi("io.kotest:kotest-runner-junit5:5.9.1")
        testApi("io.kotest:kotest-assertions-core:5.9.1")
        testApi("io.kotest:kotest-assertions-core:5.9.1")

        testApi("io.mockk:mockk:1.13.10")
        testApi(kotlin("test"))
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
                freeCompilerArgs =
                    listOf("-Xjsr305=strict", "-Xcontext-receivers", "-opt-in=kotlin.ExperimentalValueClassApi")
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }
}
