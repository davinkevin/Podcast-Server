package com.github.davinkevin.podcastserver.tag

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

class TagService(private val repository: TagRepository) {

    fun findById(id: UUID): Mono<Tag> = repository.findById(id)
    fun findByNameLike(name: String): Flux<Tag> = repository.findByNameLike(name)
}
