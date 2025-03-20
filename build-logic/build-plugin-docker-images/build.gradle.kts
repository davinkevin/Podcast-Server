plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.1.20"
}

group = "com.gitlab.davinkevin.podcastserver.dockerimages"
version = "2025.2.0"

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
