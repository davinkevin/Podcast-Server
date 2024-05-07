plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.24"
}

group = "com.gitlab.davinkevin.podcastserver.database"
version = "2024.5.0"

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
