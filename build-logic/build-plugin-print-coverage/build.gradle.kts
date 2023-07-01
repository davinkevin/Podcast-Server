plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.7.22"
}

group = "com.gitlab.davinkevin.podcastserver.printcoverage"
version = "2023.7.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jdom:jdom2:2.0.6.1")
}

gradlePlugin {
    plugins {
        create("PrintCoveragePlugin") {
            id = "build-plugin-print-coverage"
            implementationClass = "com.gitlab.davinkevin.podcastserver.printcoverage.PrintCoveragePlugin"
        }
    }
}
