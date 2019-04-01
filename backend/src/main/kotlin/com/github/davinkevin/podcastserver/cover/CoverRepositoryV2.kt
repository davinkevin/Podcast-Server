package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.database.tables.Cover.*
import com.github.davinkevin.podcastserver.extension.repository.executeAsyncAsMono
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.net.URI
import java.util.*

@Service
class CoverRepositoryV2(private val query: DSLContext) {
    fun save(cover: CoverForCreation): Mono<Cover> {
        val id = UUID.randomUUID()
        return query.insertInto(COVER)
                .set(COVER.ID, id)
                .set(COVER.WIDTH, cover.width)
                .set(COVER.HEIGHT, cover.height)
                .set(COVER.URL, cover.url.toASCIIString())
                .executeAsyncAsMono()
                .map { Cover(id, cover.url, cover.height, cover.width) }
    }
}

data class CoverForCreation(val width: Int, val height: Int, val url: URI)
