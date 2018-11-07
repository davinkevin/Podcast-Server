package com.github.davinkevin.podcastserver.business

import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import io.vavr.collection.List
import lan.dk.podcastserver.entity.Tag
import lan.dk.podcastserver.repository.TagRepository
import org.springframework.stereotype.Component
import java.util.*

/**
 * Created by kevin on 07/06/2014.
 */
@Component
class TagBusiness(val tagRepository: TagRepository) {

    fun findAll(): List<Tag> = tagRepository.findAll().toVΛVΓ()

    fun findOne(id: UUID): Tag =
            tagRepository.findById(id).k()
                    .getOrElse{ throw RuntimeException("Tag with ID $id not found") }

    fun findByNameLike(name: String): io.vavr.collection.Set<Tag> =
            tagRepository.findByNameContainsIgnoreCase(name)

    fun getTagListByName(tagList: Set<Tag>): Set<Tag> =
            tagList.map { findByName(it.name) }.toSet()

    fun findAllByName(names: io.vavr.collection.Set<String>): io.vavr.collection.Set<Tag> =
            names.flatMap { tagRepository.findByNameIgnoreCase(it) }

    private fun findByName(n: String): Tag =
            tagRepository.findByNameIgnoreCase(n)
                    .getOrElse { tagRepository.save(Tag().apply { name = n }) }

}
