package com.github.davinkevin.podcastserver.tag

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*
import com.github.davinkevin.podcastserver.tag.TagRepositoryV2 as TagRepository

@Service
class TagService(private val repository: TagRepository) {

    fun findById(id: UUID): Mono<Tag> = repository.findById(id)

}
