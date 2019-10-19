package com.github.davinkevin.podcastserver.tag

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
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
                .flatMap { ok().bodyValue(it) }
                .switchIfEmpty { notFound().build() }
    }

    fun findByNameLike(s: ServerRequest): Mono<ServerResponse> {
        val name = s.queryParam("name").orElse("")

        return tagService
                .findByNameLike(name)
                .map { TagHAL(it.id, it.name) }
                .collectList()
                .map { TagsResponse(it) }
                .flatMap { ok().bodyValue(it) }
    }
}

class TagHAL(val id: UUID, val name: String)

private class TagsResponse(val content: Collection<TagHAL>)
