package com.gitlab.davinkevin.podcastserver.dockerimages

data class Refs(val branches: List<String>, val tags: List<String>) {
    val all: List<String> get() = branches + tags
}

object TagPolicy {

    private const val SLUG_MAX_LENGTH = 63

    private val ALWAYS_KEEP = setOf("latest", "main")

    private val VERSION_SHAPED = Regex("""^v?\d+\.\d+(\.\d+)?([.-][0-9A-Za-z]+)*$""")

    const val MIN_TRUSTED_REFS = 10

    fun slugify(ref: String): String = ref
        .lowercase()
        .map { if (it in '0'..'9' || it in 'a'..'z') it else '-' }
        .joinToString("")
        .take(SLUG_MAX_LENGTH)
        .trim('-')

    fun keepList(refs: Refs): Set<String> =
        refs.all.toSet() + refs.all.map(::slugify) + ALWAYS_KEEP

    fun orphans(tags: List<String>, refs: Refs): List<String> {
        val keep = keepList(refs)
        return tags
            .filterNot { it in keep }
            .filterNot { VERSION_SHAPED.matches(it) }
            .distinct()
            .sorted()
    }

    fun isTrustworthy(refs: Refs): Boolean = refs.all.size >= MIN_TRUSTED_REFS
}
