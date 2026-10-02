plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.assignment") version "2.4.20"
}

group = "com.gitlab.davinkevin.podcastserver.dockerimages"
version = "2026.10.0"

assignment {
    annotation("org.gradle.api.SupportsKotlinAssignmentOverloading")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(localGroovy())
    implementation(gradleKotlinDsl())

    testImplementation(kotlin("test"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<org.gradle.plugin.devel.tasks.ValidatePlugins>().configureEach {
    enableStricterValidation = true
}

gradlePlugin {
    plugins {
        create("DockerImagePlugin") {
            id = "build-plugin-docker-images"
            implementationClass = "com.gitlab.davinkevin.podcastserver.dockerimages.DockerImagePlugin"
        }
    }
}
