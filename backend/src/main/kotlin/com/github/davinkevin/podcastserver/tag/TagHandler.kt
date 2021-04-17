package com.github.davinkevin.podcastserver.tag

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.util.*

/**
 * Created by kevin on 2019-03-19
 */
class TagHandler(private val tagService: TagService) {

    suspend fun findById(s: ServerRequest): ServerResponse {
        val id = UUID.fromString(s.pathVariable("id"))

        val tag = tagService.findById(id)
            ?: return notFound().buildAndAwait()

        return ok().bodyValueAndAwait(
            TagHAL(tag.id, tag.name)
        )
    }

    suspend fun findByNameLike(s: ServerRequest): ServerResponse {
        val name = s.queryParam("name").orElse("")

        val tags = tagService.findByNameLike(name)
            .map { TagHAL(it.id, it.name) }
            .toList()

        return ok().bodyValueAndAwait(TagsResponse(tags))
    }
}

private class TagHAL(val id: UUID, val name: String)
private class TagsResponse(val content: Collection<TagHAL>)
