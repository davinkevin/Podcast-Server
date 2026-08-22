package com.gitlab.davinkevin.podcastserver.dockerimages

import groovy.json.JsonOutput
import java.time.Duration

enum class ImageRegistry {
    DOCKER_HUB,
}

data class RegistryCredentials(val user: String, val password: String)

interface RegistryClient {
    fun tags(image: String): List<String>
    fun deleteTag(image: String, tag: String): Status
}

fun registryClient(
    registry: ImageRegistry,
    http: Http,
    namespace: String,
    credentials: RegistryCredentials?,
    log: (String) -> Unit,
): RegistryClient = when (registry) {
    ImageRegistry.DOCKER_HUB -> DockerHub(http, namespace, credentials, log)
}

class DockerHub(
    private val http: Http,
    private val namespace: String,
    private val credentials: RegistryCredentials?,
    private val log: (String) -> Unit,
) : RegistryClient {

    private companion object {
        const val API = "https://hub.docker.com/v2"
        const val PAGE_SIZE = 100
        const val MAX_ATTEMPTS = 5
        val FALLBACK_BACKOFF: Duration = Duration.ofSeconds(30)
    }

    private var token: String? = null

    override fun tags(image: String): List<String> = buildList {
        var url: String? = "$API/repositories/$namespace/$image/tags?page_size=$PAGE_SIZE"
        var isFirstPage = true

        while (url != null) {
            val body = when {
                isFirstPage -> http.getJson(url) as Map<*, *>
                else -> http.getJsonOrNull(url) as? Map<*, *> ?: break
            }
            val names = (body["results"] as List<*>).map { (it as Map<*, *>)["name"] as String }
            addAll(names)

            url = when {
                names.size < PAGE_SIZE -> null
                else -> body["next"] as? String
            }
            isFirstPage = false
        }
    }

    override fun deleteTag(image: String, tag: String): Status {
        var attempt = 1
        while (true) {
            val status = http.delete(
                "$API/repositories/$namespace/$image/tags/$tag/",
                mapOf("Authorization" to "JWT ${token()}"),
            )

            val lastAttempt = attempt >= MAX_ATTEMPTS
            when {
                lastAttempt -> return status
                status.isUnauthorized -> {
                    log("token rejected (${status.code}), authenticating again")
                    token = null
                }
                status.isThrottled -> {
                    val wait = status.retryAfter ?: FALLBACK_BACKOFF.multipliedBy(attempt.toLong())
                    log("throttled (${status.code}), waiting ${wait.toSeconds()}s")
                    Thread.sleep(wait.toMillis())
                }
                else -> return status
            }
            attempt++
        }
    }

    private fun token(): String = token ?: authenticate().also { token = it }

    private fun authenticate(): String {
        val given = credentials
            ?: error("Deleting from $namespace requires DOCKER_IO_USER and DOCKER_IO_PASSWORD")
        val body = JsonOutput.toJson(mapOf("username" to given.user, "password" to given.password))
        val answer = http.postJson("$API/users/login/", body) as Map<*, *>

        return (answer["token"] as? String)?.takeIf { it.isNotBlank() }
            ?: error("${ImageRegistry.DOCKER_HUB} authentication did not return a token")
    }
}
