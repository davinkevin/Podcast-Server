package com.github.davinkevin.podcastserver.tag

import com.github.davinkevin.podcastserver.database.Tables.TAG
import org.jooq.DSLContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.util.*

class TagRepository(val query: DSLContext) {

    fun findById(id: UUID): Tag? = Mono.defer {
        query
            .select(TAG.ID, TAG.NAME)
            .from(TAG)
            .where(TAG.ID.eq(id))
            .toMono()
            .map { (id, name) -> Tag(id, name) }
    }
        .block()


    fun findByNameLike(name: String): List<Tag> {
        return Flux.from(
            query
                .select(TAG.ID, TAG.NAME)
                .from(TAG)
                .where(TAG.NAME.containsIgnoreCase(name))
                .orderBy(TAG.NAME.asc())
        )
            .map { (id, name) -> Tag(id, name) }
            .collectList()
            .block()!!
    }

    fun save(name: String): Tag {
        val result = query
            .select(TAG.ID, TAG.NAME)
            .from(TAG)
            .where(TAG.NAME.eq(name))
            .toMono()
            .block()
            ?.let { Tag(it[TAG.ID], it[TAG.NAME]) }

        if (result != null) {
            return result
        }

        val id = UUID.randomUUID()
        return query
            .insertInto(TAG)
            .set(TAG.ID, id)
            .set(TAG.NAME, name)
            .toMono()
            .map { Tag(id, name) }
            .block()!!
    }

}
