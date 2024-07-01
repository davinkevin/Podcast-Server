plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.0.0"
}

group = "com.gitlab.davinkevin.podcastserver.database"
version = "2024.7.0"

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
