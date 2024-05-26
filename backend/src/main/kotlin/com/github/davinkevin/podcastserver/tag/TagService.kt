package com.github.davinkevin.podcastserver.tag

import java.util.*

class TagService(private val repository: TagRepository) {

    fun findById(id: UUID): Tag? = repository.findById(id).block()
    fun findByNameLike(name: String): List<Tag> = repository.findByNameLike(name).collectList().block()!!
}
