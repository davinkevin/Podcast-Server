package com.gitlab.davinkevin.podcastserver.dockerimages

import groovy.json.JsonOutput

enum class ImageRegistry {
    DOCKER_HUB,
}

data class RegistryCredentials(val user: String, val password: String)

interface RegistryClient {
    fun tags(image: String): List<String>
    fun deleteTag(image: String, tag: String): Boolean
}

fun registryClient(
    registry: ImageRegistry,
    http: Http,
    namespace: String,
    credentials: RegistryCredentials?,
): RegistryClient = when (registry) {
    ImageRegistry.DOCKER_HUB -> DockerHub(http, namespace, credentials)
}

class DockerHub(
    private val http: Http,
    private val namespace: String,
    private val credentials: RegistryCredentials?,
) : RegistryClient {

    private companion object {
        const val API = "https://hub.docker.com/v2"
    }

    private val token: String by lazy {
        val given = credentials
            ?: error("Deleting from ${ImageRegistry.DOCKER_HUB} requires DOCKER_IO_USER and DOCKER_IO_PASSWORD")
        val body = JsonOutput.toJson(mapOf("username" to given.user, "password" to given.password))
        val answer = http.postJson("$API/users/login/", body) as Map<*, *>

        (answer["token"] as? String)?.takeIf { it.isNotBlank() }
            ?: error("${ImageRegistry.DOCKER_HUB} authentication did not return a token")
    }

    override fun tags(image: String): List<String> = buildList {
        var url: String? = "$API/repositories/$namespace/$image/tags?page_size=100"
        while (url != null) {
            val body = http.getJson(url) as Map<*, *>
            addAll((body["results"] as List<*>).map { (it as Map<*, *>)["name"] as String })
            url = body["next"] as? String
        }
    }

    override fun deleteTag(image: String, tag: String): Boolean {
        val code = http.delete(
            "$API/repositories/$namespace/$image/tags/$tag/",
            mapOf("Authorization" to "JWT $token"),
        )
        return code in 200..299 || code == 404
    }
}
