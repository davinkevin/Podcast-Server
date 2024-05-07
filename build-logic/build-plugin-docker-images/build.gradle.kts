plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.24"
}

group = "com.gitlab.davinkevin.podcastserver.dockerimages"
version = "2024.5.0"

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
