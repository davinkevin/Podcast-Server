package com.gitlab.davinkevin.podcastserver.dockerimages

import groovy.json.JsonSlurper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration

class Http {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun getJson(url: String): Any = JsonSlurper().parseText(send(get(url)))

    fun postJson(url: String, body: String): Any = JsonSlurper().parseText(
        send(
            request(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        )
    )

    fun delete(url: String, headers: Map<String, String>): Int {
        val builder = request(url).DELETE()
        headers.forEach { (name, value) -> builder.header(name, value) }
        return client.send(builder.build(), BodyHandlers.discarding()).statusCode()
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
