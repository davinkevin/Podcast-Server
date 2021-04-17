package com.github.davinkevin.podcastserver.tag

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

class TagService(private val repository: TagRepository) {

    suspend fun findById(id: UUID): Tag? = repository.findById(id)
    fun findByNameLike(name: String): Flow<Tag> = repository.findByNameLike(name)
}
