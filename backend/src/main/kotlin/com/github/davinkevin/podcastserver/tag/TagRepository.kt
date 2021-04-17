package com.github.davinkevin.podcastserver.tag

import com.github.davinkevin.podcastserver.database.Tables.TAG
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.asFlux
import org.jooq.DSLContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.util.*

class TagRepository(val query: DSLContext) {

    suspend fun findById(id: UUID): Tag? = Mono.defer {
        query
            .select(TAG.ID, TAG.NAME)
            .from(TAG)
            .where(TAG.ID.eq(id))
            .toMono()
            .map { (id, name) -> Tag(id, name) }
    }
        .subscribeOn(Schedulers.boundedElastic())
        .publishOn(Schedulers.parallel())
        .awaitFirstOrNull()

    fun findByNameLike(name: String): Flow<Tag> = Flux.defer {
        Flux.from(
            query
                .select(TAG.ID, TAG.NAME)
                .from(TAG)
                .where(TAG.NAME.containsIgnoreCase(name))
                .orderBy(TAG.NAME.asc())
        )
    }
        .map { (id, name) -> Tag(id, name) }
        .subscribeOn(Schedulers.boundedElastic())
        .publishOn(Schedulers.parallel())
        .asFlow()

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
        .subscribeOn(Schedulers.boundedElastic())
        .publishOn(Schedulers.parallel())

}
