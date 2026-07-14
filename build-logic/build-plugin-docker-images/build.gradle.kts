plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.4.10"
}

group = "com.gitlab.davinkevin.podcastserver.dockerimages"
version = "2026.1.0"

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("DockerImagePlugin") {
            id = "build-plugin-docker-images"
            implementationClass = "com.gitlab.davinkevin.podcastserver.dockerimages.DockerImagePlugin"
        }
    }
}
