plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.2.10"
}

group = "com.gitlab.davinkevin.podcastserver.database"
version = "2025.7.0"

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
