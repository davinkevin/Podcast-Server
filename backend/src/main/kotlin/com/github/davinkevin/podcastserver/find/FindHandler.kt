package com.github.davinkevin.podcastserver.find

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyToMono
import java.net.URI

class FindHandler(private val finderService: FindService) {

    fun find(r: ServerRequest) = r
            .bodyToMono<String>()
            .map { URI(it) }
            .flatMap { finderService.find(it) }
            .flatMap { ok().syncBody(it) }

}
