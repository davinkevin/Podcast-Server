plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.8.22"
}

group = "com.gitlab.davinkevin.podcastserver.dockerimages"
version = "2023.10.0"

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
