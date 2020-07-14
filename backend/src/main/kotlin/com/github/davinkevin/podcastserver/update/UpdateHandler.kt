package com.github.davinkevin.podcastserver.update

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import java.util.*

class UpdateHandler(
        private val update: UpdateService
) {

    fun updateAll(r: ServerRequest): Mono<ServerResponse> {
        val force = r.queryParam("force").map { it!!.toBoolean() }.orElse(false)
        val withDownload = r.queryParam("download").map { it!!.toBoolean() }.orElse(false)

        return update
                .updateAll(force, withDownload)
                .then(ok().build())
    }

    fun update(r: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(r.pathVariable("podcastId"))

        return update
                .update(id)
                .then(ok().build())
    }
}
