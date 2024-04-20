package com.github.davinkevin.podcastserver.podcast.type

import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

class TypeHandler(updaterSelector: UpdaterSelector) {

    private val types by lazy {
        updaterSelector.types()
        .map { TypeResponse.TypeHAL(it.key, it.name) }
        .let(::TypeResponse)
    }

    fun findAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse =
        ServerResponse.ok().body(types)
}

private class TypeResponse(@Suppress("unused") val content: Collection<TypeHAL>) {
    data class TypeHAL(val key: String, val name: String)
}

