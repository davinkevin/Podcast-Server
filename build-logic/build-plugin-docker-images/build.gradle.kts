plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.1.0"
}

group = "com.gitlab.davinkevin.podcastserver.dockerimages"
version = "2025.1.0"

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
