package com.github.davinkevin.podcastserver.tag

import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.paramOrNull
import java.util.*

/**
 * Created by kevin on 2019-03-19
 */
class TagHandler(private val tagService: TagService) {

    fun findById(s: ServerRequest): ServerResponse {
        val id = s.pathVariable("id")
            .let(UUID::fromString)

        val tag = tagService.findById(id)
            ?: return ServerResponse.notFound().build()

        val body = TagHAL(tag.id, tag.name)

        return ServerResponse.ok().body(body)
    }

    fun findByNameLike(s: ServerRequest): ServerResponse {
        val name = s.paramOrNull("name") ?: ""

        val tags = tagService.findByNameLike(name)

        val body = tags
            .map { TagHAL(it.id, it.name) }
            .let(::TagsResponse)

        return ServerResponse.ok().body(body)
    }
}

private class TagHAL(val id: UUID, val name: String)
private class TagsResponse(val content: Collection<TagHAL>)
