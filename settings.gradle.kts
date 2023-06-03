rootProject.name = "Podcast-Server"

plugins {
    id("com.gradle.enterprise") version("3.13.2")
    id("com.gradle.common-custom-user-data-gradle-plugin") version "1.10"
}

val isCI = System.getenv("CI").toBoolean()
buildCache {
    local { isEnabled = !isCI }
    remote(gradleEnterprise.buildCache) { isPush = isCI }
}

gradleEnterprise {
    server = "https://gradle-enterprise.davinkevin.fr"
    buildScan {
        publishAlways()
        capture {
            isTaskInputFiles = true
            isUploadInBackground = !isCI
        }
        if(isCI) {
            tag(System.getenv("CI_COMMIT_REF_NAME"))
            value("Pipeline", System.getenv("CI_PIPELINE_ID"))
            value("Job Image", System.getenv("CI_JOB_IMAGE"))
            link("Source", "https://gitlab.com/davinkevin/Podcast-Server/tree/${System.getenv("CI_COMMIT_REF_NAME")}")
        }
    }
}


include("backend-lib-database")
include("backend-lib-youtubedl")
include("backend")
include("frontend-angular")
include("frontend-angularjs")

includeBuild("build-logic/build-plugin-database") { name = "build-plugin-database" }
includeBuild("build-logic/build-plugin-docker-images") { name = "build-plugin-docker-images" }
includeBuild("build-logic/build-plugin-print-coverage") { name = "build-plugin-print-coverage" }
