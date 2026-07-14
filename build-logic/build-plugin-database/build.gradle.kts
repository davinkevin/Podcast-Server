plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.4.10"
}

group = "com.gitlab.davinkevin.podcastserver.database"
version = "2026.1.0"

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
