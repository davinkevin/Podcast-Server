package com.github.davinkevin.podcastserver.business

import com.github.davinkevin.podcastserver.entity.Tag
import lan.dk.podcastserver.repository.TagRepository
import org.springframework.stereotype.Component

/**
 * Created by kevin on 07/06/2014.
 */
@Component
class TagBusiness(val tagRepository: TagRepository) {

    fun getTagListByName(tagList: Set<Tag>): Set<Tag> =
            tagList
                    .mapNotNull { it.name }
                    .map { findByName(it) }
                    .toSet()

    fun findAllByName(names: io.vavr.collection.Set<String>): io.vavr.collection.Set<Tag> =
            names.flatMap { tagRepository.findByNameIgnoreCase(it) }

    private fun findByName(n: String): Tag =
            tagRepository.findByNameIgnoreCase(n)
                    .getOrElse { tagRepository.save(Tag().apply { name = n }) }

}
