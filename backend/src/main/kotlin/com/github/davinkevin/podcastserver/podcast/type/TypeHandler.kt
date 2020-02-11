package com.github.davinkevin.podcastserver.podcast.type

import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok

class TypeHandler(updaterSelector: UpdaterSelector) {

    private val types by lazy { updaterSelector.types().map { TypeHAL(it.key, it.name) } }

    fun findAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest) = ok().bodyValue(TypeResponse(types))
}

data class TypeHAL(val key: String, val name: String)
class TypeResponse(val content: Collection<TypeHAL>)
