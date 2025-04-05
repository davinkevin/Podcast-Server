plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.1.20"
}

group = "com.gitlab.davinkevin.podcastserver.database"
version = "2025.4.0"

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
