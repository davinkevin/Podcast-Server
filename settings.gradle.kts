rootProject.name = "Podcast-Server"

plugins {
    id("com.gradle.develocity") version("3.19.2")
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

val env: Map<String, String> = System.getenv()
val isCI = env["CI"].toBoolean()
val hasDV = env["DEVELOCITY_ENABLED"].toBoolean()

buildCache {
    local { isEnabled = !isCI }
    remote(develocity.buildCache) {
        isEnabled = isCI || hasDV
        isPush = isCI
    }
}

develocity {
    server = env["DEVELOCITY_SERVER"] ?: "https://no.ge.local"
    buildScan {
        publishing.onlyIf { hasDV || isCI }
        capture {
            fileFingerprints = true
            buildLogging = true
            testLogging = true
        }
        uploadInBackground = !isCI
        if(isCI) {
            tag(env["CI_COMMIT_REF_NAME"])
            value("Pipeline", env["CI_PIPELINE_ID"])
            value("Job Image", env["CI_JOB_IMAGE"])
            link("Source", "https://gitlab.com/davinkevin/Podcast-Server/tree/${env["CI_COMMIT_REF_NAME"]}")
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
