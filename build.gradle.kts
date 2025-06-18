import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

allprojects {
    apply {
        plugin("maven-publish")
    }
    group = "io.clroot.ball"
    version = "2.0.0-20250618.9-SNAPSHOT"

    val nexusUsername =
        System.getenv("NEXUS_REPO_USERNAME")
            ?: throw GradleException(
                "NEXUS_REPO_USERNAME environment variable must be set",
            )
    val nexusPassword =
        System.getenv("NEXUS_REPO_PASSWORD")
            ?: throw GradleException(
                "NEXUS_REPO_PASSWORD environment variable must be set",
            )

    repositories {
        mavenCentral()
        maven {
            url = uri("https://nexus.eduvil.co.kr/repository/eduvil-maven-snapshot/")
            credentials {
                username = nexusUsername
                password = nexusPassword
            }
        }
    }

    publishing {
        repositories {
            maven {
                val releasesRepoUrl = uri("https://nexus.eduvil.co.kr/repository/eduvil-maven-release/")
                val snapshotsRepoUrl = uri("https://nexus.eduvil.co.kr/repository/eduvil-maven-snapshot/")
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
