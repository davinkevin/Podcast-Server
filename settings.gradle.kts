rootProject.name = "Podcast-Server"

plugins {
    id("com.gradle.enterprise") version("3.16")
    id("com.gradle.common-custom-user-data-gradle-plugin") version "1.12"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

val env: Map<String, String> = System.getenv()
val isCI = env["CI"].toBoolean()
val hasGE = env["GRADLE_ENTERPRISE_ENABLED"].toBoolean()

buildCache {
    local { isEnabled = !isCI }
    remote(gradleEnterprise.buildCache) {
        isEnabled = isCI || hasGE
        isPush = isCI
    }
}

gradleEnterprise {
    server = env["GRADLE_ENTERPRISE_SERVER"] ?: "https://no.ge.local"
    buildScan {
        publishAlwaysIf( hasGE || isCI )
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
