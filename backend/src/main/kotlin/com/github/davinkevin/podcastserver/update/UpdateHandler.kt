package com.github.davinkevin.podcastserver.update

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.buildAndAwait
import reactor.core.publisher.Mono
import java.util.*

class UpdateHandler(
        private val update: UpdateService
) {

    suspend fun updateAll(r: ServerRequest): ServerResponse {
        val force = r.queryParam("force").map { it.toBoolean() }.orElse(false)
        val withDownload = r.queryParam("download").map { it.toBoolean() }.orElse(false)

        update.updateAll(force, withDownload).awaitFirstOrNull()

        return ok().buildAndAwait()
    }

    suspend fun update(r: ServerRequest): ServerResponse {
        val id = UUID.fromString(r.pathVariable("podcastId"))

        update.update(id).awaitFirstOrNull()

        return ok().buildAndAwait()
    }
}
