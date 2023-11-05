import org.gradle.internal.deprecation.DeprecatableConfiguration

plugins {
    id("java")
}

group = "com.gitlab.davinkevin.podcastserver.youtubedl"
version = "2023.11.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.register("downloadDependencies") {
    fun Configuration.isDeprecated(): Boolean = when (this) {
        is DeprecatableConfiguration -> resolutionAlternatives != null
        else -> false
    }
    doLast {
        val buildDeps = buildscript
            .configurations
            .sumOf { it.resolve().size }

        val allDeps = configurations
            .filter { it.isCanBeResolved && !it.isDeprecated() }
            .sumOf { it.resolve().size }

        println("Downloaded all dependencies: ${allDeps + buildDeps}")
    }
}
