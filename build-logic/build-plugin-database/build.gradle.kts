plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.0.20"
}

group = "com.gitlab.davinkevin.podcastserver.database"
version = "2024.8.0"

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("DatabasePlugin") {
            id = "build-plugin-database"
            implementationClass = "com.gitlab.davinkevin.podcastserver.database.DatabasePlugin"
        }
    }
}
