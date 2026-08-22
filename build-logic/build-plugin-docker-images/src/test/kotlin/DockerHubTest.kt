package com.gitlab.davinkevin.podcastserver.dockerimages

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val NO_WAIT: Duration = Duration.ZERO

private fun page(names: List<String>, next: String?) =
    mapOf("results" to names.map { mapOf("name" to it) }, "next" to next)

class FakeHttp(
    private val deleteAnswers: MutableList<Status> = mutableListOf(),
    private val pages: MutableList<Any?> = mutableListOf(),
) : Http {

    val logins = mutableListOf<String>()
    val deletes = mutableListOf<String>()
    val gets = mutableListOf<String>()

    override fun getJson(url: String): Any {
        gets += url
        return pages.removeFirst() ?: error("$url answered 404")
    }

    override fun getJsonOrNull(url: String): Any? {
        gets += url
        return pages.removeFirst()
    }

    override fun postJson(url: String, body: String): Any {
        logins += url
        return mapOf("token" to "token-${logins.size}")
    }

    override fun delete(url: String, headers: Map<String, String>): Status {
        deletes += headers.getValue("Authorization")
        return deleteAnswers.removeFirst()
    }
}

private fun dockerHub(http: FakeHttp) =
    DockerHub(http, "podcastserver", RegistryCredentials("user", "password")) {}

private fun dockerHubDeleting(vararg answers: Status): Pair<DockerHub, FakeHttp> {
    val http = FakeHttp(deleteAnswers = answers.toMutableList())
    return dockerHub(http) to http
}

class DockerHubTest {

    @Test
    fun `a deleted tag reports success`() {
        val (registry, http) = dockerHubDeleting(Status(204, null))

        assertTrue(registry.deleteTag("ui", "gone-branch").isSuccess)
        assertEquals(1, http.deletes.size)
    }

    @Test
    fun `an expired token is renewed and the delete retried`() {
        val (registry, http) = dockerHubDeleting(Status(401, null), Status(204, null))

        assertTrue(registry.deleteTag("ui", "gone-branch").isSuccess)
        assertEquals(2, http.logins.size)
        assertEquals(listOf("JWT token-1", "JWT token-2"), http.deletes)
    }

    @Test
    fun `a 403 is treated as an expired token too`() {
        val (registry, http) = dockerHubDeleting(Status(403, null), Status(204, null))

        assertTrue(registry.deleteTag("ui", "gone-branch").isSuccess)
        assertEquals(2, http.logins.size)
    }

    @Test
    fun `throttling is retried after the delay the registry asks for`() {
        val (registry, http) = dockerHubDeleting(Status(429, NO_WAIT), Status(204, null))

        assertTrue(registry.deleteTag("ui", "gone-branch").isSuccess)
        assertEquals(2, http.deletes.size)
        assertEquals(1, http.logins.size)
    }

    @Test
    fun `persistent throttling gives up and reports the status`() {
        val (registry, http) = dockerHubDeleting(*Array(6) { Status(429, NO_WAIT) })

        assertEquals(429, registry.deleteTag("ui", "gone-branch").code)
        assertEquals(5, http.deletes.size)
    }

    @Test
    fun `an unexpected status is not retried`() {
        val (registry, http) = dockerHubDeleting(Status(500, null))

        assertEquals(500, registry.deleteTag("ui", "gone-branch").code)
        assertEquals(1, http.deletes.size)
    }

    @Test
    fun `a tag already gone counts as reconciled`() {
        val (registry, _) = dockerHubDeleting(Status(404, null))

        assertTrue(registry.deleteTag("ui", "gone-branch").isGone)
    }

    @Test
    fun `a single short page is the whole listing`() {
        val http = FakeHttp(pages = mutableListOf(page(listOf("main", "latest"), null)))

        assertEquals(listOf("main", "latest"), dockerHub(http).tags("ui"))
        assertEquals(1, http.gets.size)
    }

    @Test
    fun `a short page ends the listing even when a next page is advertised`() {
        // Docker Hub keeps advertising `next` from a stale count after a bulk
        // delete: it announced 213 tags while page 1 held the 87 that remained.
        val http = FakeHttp(pages = mutableListOf(page(listOf("main"), "https://hub.docker.com/page-2")))

        assertEquals(listOf("main"), dockerHub(http).tags("ui"))
        assertEquals(1, http.gets.size)
    }

    @Test
    fun `a next page that no longer exists ends the listing instead of failing`() {
        val full = (1..100).map { "tag-$it" }
        val http = FakeHttp(pages = mutableListOf(page(full, "https://hub.docker.com/page-2"), null))

        assertEquals(full, dockerHub(http).tags("ui"))
        assertEquals(2, http.gets.size)
    }

    @Test
    fun `two full pages are both read`() {
        val first = (1..100).map { "tag-$it" }
        val http = FakeHttp(
            pages = mutableListOf(
                page(first, "https://hub.docker.com/page-2"),
                page(listOf("last"), null),
            )
        )

        assertEquals(first + "last", dockerHub(http).tags("ui"))
    }
}
