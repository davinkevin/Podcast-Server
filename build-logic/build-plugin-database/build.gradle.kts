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
        create("DatabasePlugin") {
            id = "build-plugin-database"
            implementationClass = "com.gitlab.davinkevin.podcastserver.database.DatabasePlugin"
        }
    }
}
