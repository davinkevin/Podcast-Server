@file:Suppress("unused")

package com.gitlab.davinkevin.podcastserver.dockerimages

import org.gradle.api.Plugin
import org.gradle.api.Project
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
