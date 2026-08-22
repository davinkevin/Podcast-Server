package com.gitlab.davinkevin.podcastserver.dockerimages

class GitLabRefs(
    private val http: Http,
    private val apiUrl: String,
    private val projectId: String,
) {
    fun fetch() = Refs(branches = names("branches"), tags = names("tags"))

    private fun names(collection: String): List<String> = buildList {
        var page = 1
        while (true) {
            val body = http.getJson(
                "$apiUrl/projects/$projectId/repository/$collection?per_page=100&page=$page"
            ) as List<*>
            addAll(body.map { (it as Map<*, *>)["name"] as String })
            if (body.size < 100) return@buildList
            page++
        }
    }
}
