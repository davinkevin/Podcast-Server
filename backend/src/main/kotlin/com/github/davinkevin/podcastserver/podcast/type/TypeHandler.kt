package com.github.davinkevin.podcastserver.podcast.type

import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import com.github.davinkevin.podcastserver.manager.worker.Type
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.toMono

@Component
class TypeHandler(updaterSelector: UpdaterSelector) {

    private val types by lazy { updaterSelector.types().map { TypeHAL(it.key, it.name) } }

    fun findAll(r: ServerRequest) = ok().syncBody(TypeResponse(types))
}

data class TypeHAL(val key: String, val name: String)
class TypeResponse(val content: Collection<TypeHAL>)
