package com.github.davinkevin.podcastserver.tag

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import reactor.core.publisher.switchIfEmpty
import java.util.*

/**
 * Created by kevin on 2019-03-19
 */
@Component
class TagHandler(private val tagService: TagService) {

    fun findById(s: ServerRequest): Mono<ServerResponse> {
        val id = UUID.fromString(s.pathVariable("id"))

        return tagService
                .findById(id)
                .map { TagHAL(it.id, it.name) }
                .flatMap { ok().syncBody(it) }
                .switchIfEmpty { notFound().build() }
    }
}

class TagHAL(val id: UUID, val name: String)
