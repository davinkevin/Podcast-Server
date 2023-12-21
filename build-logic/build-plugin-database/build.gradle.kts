plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.22"
}

group = "com.gitlab.davinkevin.podcastserver.database"
version = "2023.12.0"

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
