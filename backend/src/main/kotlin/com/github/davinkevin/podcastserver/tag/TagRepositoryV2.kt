package com.github.davinkevin.podcastserver.tag

import com.github.davinkevin.podcastserver.database.Tables
import com.github.davinkevin.podcastserver.extension.repository.fetchOneAsMono
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.*

@Repository
class TagRepositoryV2(val query: DSLContext) {

    fun findById(id: UUID): Mono<Tag> = query
            .select(Tables.TAG.ID, Tables.TAG.NAME)
            .from(Tables.TAG)
            .where(Tables.TAG.ID.eq(id))
            .fetchOneAsMono()
            .map { Tag(it[Tables.TAG.ID], it[Tables.TAG.NAME]) }

}
