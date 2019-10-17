package com.github.davinkevin.podcastserver.tag

import com.github.davinkevin.podcastserver.database.Tables.TAG
import com.github.davinkevin.podcastserver.extension.repository.executeAsyncAsMono
import com.github.davinkevin.podcastserver.extension.repository.fetchAsFlux
import com.github.davinkevin.podcastserver.extension.repository.fetchOneAsMono
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.core.publisher.switchIfEmpty
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers
import java.util.*

@Repository
class TagRepositoryV2(val query: DSLContext) {

    fun findById(id: UUID) = query
            .select(TAG.ID, TAG.NAME)
            .from(TAG)
            .where(TAG.ID.eq(id))
            .fetchOneAsMono()
            .map { Tag(it[TAG.ID], it[TAG.NAME]) }

    fun findByNameLike(name: String) = query
            .select(TAG.ID, TAG.NAME)
            .from(TAG)
            .where(TAG.NAME.containsIgnoreCase(name))
            .orderBy(TAG.NAME.asc())
            .fetchAsFlux()
            .map { Tag(it[TAG.ID], it[TAG.NAME]) }

    fun save(name: String): Mono<Tag> = Mono.defer {
        query
                .select(TAG.ID, TAG.NAME)
                .from(TAG)
                .where(TAG.NAME.eq(name))
                .fetchOneAsMono()
                .map { Tag(it[TAG.ID], it[TAG.NAME]) }
                .switchIfEmpty {
                    val id = UUID.randomUUID()
                    query
                            .insertInto(TAG)
                            .set(TAG.ID, id)
                            .set(TAG.NAME, name)
                            .executeAsyncAsMono()
                            .map { Tag(id, name) }
                }
    }
            .subscribeOn(Schedulers.elastic())

}
