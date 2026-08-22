@file:Suppress("unused")

package com.gitlab.davinkevin.podcastserver.dockerimages

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.assign
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Created by kevin on 26/12/2022
 */
class DockerImagePlugin: Plugin<Project> {
    override fun apply(project: Project) {

        val env: Map<String, String> = System.getenv()!!
        val providedTag = project.findProperty("tag")?.toString()
        val isCI = env["CI"].toBoolean()

        val images = when {
            env["SKAFFOLD"].toBoolean() -> setOf("main")
            isCI -> generateTagsListForCI()
            providedTag != null -> setOf(providedTag)
            else -> setOf(OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS"))!!)
        }

        project.extensions.add(DockerImagesConfiguration::class.java, "dockerImagesConfiguration", DockerImagesConfiguration(images))

        val imageCleanup = project.extensions.create("imageCleanup", ImageCleanupExtension::class.java)
        val providers = project.providers

        project.tasks.register("cleanupImageTags", CleanupImageTagsTask::class.java) { task ->
            task.group = "docker"
            task.description = "Deletes the registry tags that no longer match a git ref"

            task.registry = imageCleanup.registry
            task.namespace = imageCleanup.namespace
            task.images = imageCleanup.images
            task.apiUrl = providers.environmentVariable("CI_API_V4_URL").orElse("https://gitlab.com/api/v4")
            task.projectId = providers.environmentVariable("CI_PROJECT_ID").orElse("13640563")
            task.registryUser = providers.environmentVariable("DOCKER_IO_USER")
            task.registryPassword = providers.environmentVariable("DOCKER_IO_PASSWORD")
            task.dryRun = providers.environmentVariable("DRY_RUN").map { it.toBoolean() }.orElse(false)
        }
    }

    private fun generateTagsListForCI(): Set<String> {
        val env: Map<String, String> = System.getenv()!!
        val ciCommitTag = env["CI_COMMIT_TAG"]
        val ciCommitRefSlug = env["CI_COMMIT_REF_SLUG"] ?: error("CI_COMMIT_REF_SLUG not defined")
        val ciDefaultBranch = env["CI_DEFAULT_BRANCH"] ?: error("CI_DEFAULT_BRANCH not defined")

        if (ciCommitTag != null) {
            return setOf(ciCommitTag)
        }

        if(ciCommitRefSlug != ciDefaultBranch) {
            return setOf(ciCommitRefSlug)
        }

        return setOf(ciCommitRefSlug, "latest")
    }
}

data class DockerImagesConfiguration(val tags: Set<String>)

abstract class ImageCleanupExtension {
    abstract val registry: Property<ImageRegistry>
    abstract val namespace: Property<String>
    abstract val images: ListProperty<String>
}
