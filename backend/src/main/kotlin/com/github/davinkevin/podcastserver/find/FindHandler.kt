package com.github.davinkevin.podcastserver.find

import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.body
import java.net.URI

class FindHandler(
    private val finderService: FindService
) {

    fun find(r: ServerRequest): ServerResponse {
        val uri = r.body<String>().let(URI::create)
        val podcastMetadata = finderService.find(uri)
        return ServerResponse.ok().body(podcastMetadata)
    }

}
