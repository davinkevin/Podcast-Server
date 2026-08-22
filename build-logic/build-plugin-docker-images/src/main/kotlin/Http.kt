package com.gitlab.davinkevin.podcastserver.dockerimages

import groovy.json.JsonSlurper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration

data class Status(val code: Int, val retryAfter: Duration?) {
    val isSuccess: Boolean get() = code in 200..299
    val isGone: Boolean get() = code == 404
    val isUnauthorized: Boolean get() = code == 401 || code == 403
    val isThrottled: Boolean get() = code == 429
}

interface Http {
    fun getJson(url: String): Any
    fun getJsonOrNull(url: String): Any?
    fun postJson(url: String, body: String): Any
    fun delete(url: String, headers: Map<String, String>): Status
}

class JdkHttp : Http {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    override fun getJson(url: String): Any = JsonSlurper().parseText(send(get(url)))

    override fun getJsonOrNull(url: String): Any? {
        val response = client.send(get(url), BodyHandlers.ofString())
        if (response.statusCode() == 404) return null
        if (response.statusCode() !in 200..299) {
            error("$url answered ${response.statusCode()}: ${response.body().take(500)}")
        }
        return JsonSlurper().parseText(response.body())
    }

    override fun postJson(url: String, body: String): Any = JsonSlurper().parseText(
        send(
            request(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        )
    )

    override fun delete(url: String, headers: Map<String, String>): Status {
        val builder = request(url).DELETE()
        headers.forEach { (name, value) -> builder.header(name, value) }
        val response = client.send(builder.build(), BodyHandlers.discarding())

        val retryAfter = response.headers()
            .firstValue("Retry-After")
            .orElse(null)
            ?.toLongOrNull()
            ?.let(Duration::ofSeconds)

        return Status(response.statusCode(), retryAfter)
    }

    private fun get(url: String) = request(url).GET().build()

    private fun request(url: String) = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(60))

    private fun send(request: HttpRequest): String {
        val response = client.send(request, BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("${request.uri()} answered ${response.statusCode()}: ${response.body().take(500)}")
        }
        return response.body()
    }
}
