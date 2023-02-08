plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.7.22"
}

group = "com.gitlab.davinkevin.podcastserver"
version = "2023.2.0"

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
