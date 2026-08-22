package com.gitlab.davinkevin.podcastserver.dockerimages

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Reconciles a remote registry, so it has no cacheable output")
abstract class CleanupImageTagsTask : DefaultTask() {

    @get:Input
    abstract val registry: Property<ImageRegistry>

    @get:Input
    abstract val namespace: Property<String>

    @get:Input
    abstract val images: ListProperty<String>

    @get:Input
    abstract val apiUrl: Property<String>

    @get:Input
    abstract val projectId: Property<String>

    @get:Input
    @get:Optional
    abstract val registryUser: Property<String>

    @get:Input
    @get:Optional
    abstract val registryPassword: Property<String>

    @get:Input
    abstract val dryRun: Property<Boolean>

    @TaskAction
    fun reconcile() {
        val http = Http()
        val isDryRun = dryRun.get()

        val refs = GitLabRefs(http, apiUrl.get(), projectId.get()).fetch()
        require(TagPolicy.isTrustworthy(refs)) {
            "Only ${refs.all.size} refs listed, expected at least ${TagPolicy.MIN_TRUSTED_REFS}. " +
                "Refusing to delete anything: the GitLab listing looks incomplete."
        }
        logger.lifecycle("${refs.branches.size} branches, ${refs.tags.size} tags")

        val credentials = when {
            isDryRun -> null
            else -> RegistryCredentials(registryUser.get(), registryPassword.get())
        }
        val target = registryClient(registry.get(), http, namespace.get(), credentials)

        var deleted = 0
        val failures = mutableListOf<String>()

        images.get().forEach { image ->
            val tags = target.tags(image)
            val orphans = TagPolicy.orphans(tags, refs)
            logger.lifecycle("${namespace.get()}/$image: ${tags.size} tags, ${orphans.size} orphaned")

            orphans.forEach { tag ->
                when {
                    isDryRun -> logger.lifecycle("  would delete $tag")
                    target.deleteTag(image, tag) -> deleted++
                    else -> failures += "$image:$tag"
                }
            }
        }

        when {
            isDryRun -> logger.lifecycle("Dry run, nothing deleted")
            failures.isEmpty() -> logger.lifecycle("Deleted $deleted tags")
            else -> error("Deleted $deleted tags, ${failures.size} failed: ${failures.joinToString()}")
        }
    }
}
