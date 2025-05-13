plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.1.21"
}

group = "com.gitlab.davinkevin.podcastserver.database"
version = "2025.5.0"

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
