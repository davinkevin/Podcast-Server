package com.github.davinkevin.podcastserver.tag

import com.github.davinkevin.podcastserver.database.Tables.TAG
import org.jooq.DSLContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.util.*

class TagRepository(val query: DSLContext) {

    fun findById(id: UUID): Mono<Tag> = Mono.defer {
        query
                .select(TAG.ID, TAG.NAME)
                .from(TAG)
                .where(TAG.ID.eq(id))
                .toMono()
                .map { Tag(it[TAG.ID], it[TAG.NAME]) }
    }

    fun findByNameLike(name: String): Flux<Tag> {
        return Flux.from(
                query
                        .select(TAG.ID, TAG.NAME)
                        .from(TAG)
                        .where(TAG.NAME.containsIgnoreCase(name))
                        .orderBy(TAG.NAME.asc())
        )
                .map { Tag(it[TAG.ID], it[TAG.NAME]) }
    }

    fun save(name: String): Mono<Tag> = Mono.defer {
        query
                .select(TAG.ID, TAG.NAME)
                .from(TAG)
                .where(TAG.NAME.eq(name))
                .toMono()
                .map { Tag(it[TAG.ID], it[TAG.NAME]) }
                .switchIfEmpty {
                    val id = UUID.randomUUID()
                    query
                            .insertInto(TAG)
                            .set(TAG.ID, id)
                            .set(TAG.NAME, name)
                            .toMono()
                            .map { Tag(id, name) }
                }
    }
            .subscribeOn(Schedulers.elastic())

}
