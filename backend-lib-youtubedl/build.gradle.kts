import org.gradle.internal.deprecation.DeprecatableConfiguration

plugins {
    id("org.springframework.boot") version "3.3.1"
    id("io.spring.dependency-management") version "1.1.6"

    id("java")
}

group = "com.gitlab.davinkevin.podcastserver.youtubedl"
version = "2024.7.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.register("downloadDependencies") {
    fun Configuration.isDeprecated(): Boolean = when (this) {
        is DeprecatableConfiguration -> resolutionAlternatives.isNotEmpty()
        else -> false
    }
    doLast {
        val buildDeps = buildscript
            .configurations
            .onEach { it.incoming.artifactView { lenient(true) }.artifacts }
            .sumOf { it.resolve().size }

        val allDeps = configurations
            .filter { it.isCanBeResolved && !it.isDeprecated() }
            .onEach { it.incoming.artifactView { lenient(true) }.artifacts }
            .sumOf { it.resolve().size }

        println("Downloaded all dependencies: ${allDeps + buildDeps}")
    }
}
