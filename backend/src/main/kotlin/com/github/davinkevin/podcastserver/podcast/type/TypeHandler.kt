package com.github.davinkevin.podcastserver.podcast.type

import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait

class TypeHandler(updaterSelector: UpdaterSelector) {

    private val types = updaterSelector
        .types()
        .map { TypeHAL(it.key, it.name) }

    suspend fun findAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest) =
        ok().bodyValueAndAwait(TypeResponse(types))
}

private data class TypeHAL(val key: String, val name: String)
private class TypeResponse(val content: Collection<TypeHAL>)
