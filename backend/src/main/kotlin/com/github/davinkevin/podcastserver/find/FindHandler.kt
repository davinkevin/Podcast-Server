package com.github.davinkevin.podcastserver.find

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.net.URI

class FindHandler(
    private val finderService: FindService
) {

    suspend fun find(r: ServerRequest): ServerResponse {
        val url = r.awaitBody<String>()
        val info = finderService.find(URI(url))
        return ok().bodyValueAndAwait(info)
    }

}
