import com.gradle.develocity.agent.gradle.scan.BuildScanPublishingConfiguration
import org.gradle.api.specs.Specs

rootProject.name = "Podcast-Server"

plugins {
    id("com.gradle.develocity") version("4.4.2")
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.6.0"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val isCI = providers.environmentVariable("CI").map { it.toBoolean() }.getOrElse(false)
val hasDV = providers.environmentVariable("DEVELOCITY_ENABLED").map { it.toBoolean() }.getOrElse(false)
val shouldPublish = isCI || hasDV
val publishSpec: Spec<BuildScanPublishingConfiguration.PublishingContext?> =
    if (shouldPublish) Specs.satisfyAll() else Specs.satisfyNone()

buildCache {
    local { isEnabled = !isCI }
    remote(develocity.buildCache) {
        isEnabled = shouldPublish
        isPush = isCI
    }
}

develocity {
    server = providers.environmentVariable("DEVELOCITY_SERVER").getOrElse("https://no.ge.local")
    buildScan {
        publishing.onlyIf(publishSpec)
        capture {
            fileFingerprints = true
            buildLogging = true
            testLogging = true
        }
        uploadInBackground = !isCI
        if (isCI) {
            val refName = providers.environmentVariable("CI_COMMIT_REF_NAME").getOrElse("")
            val pipelineId = providers.environmentVariable("CI_PIPELINE_ID").getOrElse("")
            val jobImage = providers.environmentVariable("CI_JOB_IMAGE").getOrElse("")
            tag(refName)
            value("Pipeline", pipelineId)
            value("Job Image", jobImage)
            link("Source", "https://gitlab.com/davinkevin/Podcast-Server/tree/$refName")
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
