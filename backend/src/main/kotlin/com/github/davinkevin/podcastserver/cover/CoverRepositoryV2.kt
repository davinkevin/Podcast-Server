package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.cover.DeleteCoverInformation.*
import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.database.Tables.PODCAST
import com.github.davinkevin.podcastserver.database.tables.Cover.*
import com.github.davinkevin.podcastserver.extension.repository.executeAsyncAsMono
import com.github.davinkevin.podcastserver.extension.repository.fetchAsFlux
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.sql.Timestamp
import java.time.OffsetDateTime
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

    fun findCoverOlderThan(date: OffsetDateTime): Flux<DeleteCoverInformation> {
        return query
                .select(
                        PODCAST.ID, PODCAST.TITLE,
                        ITEM.ID, ITEM.TITLE,
                        COVER.ID, COVER.URL
                )
                .from(COVER
                        .innerJoin(ITEM).on(COVER.ID.eq(ITEM.COVER_ID))
                        .innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID))
                )
                .where(ITEM.CREATION_DATE.lessOrEqual(Timestamp.valueOf(date.toLocalDateTime())))
                .orderBy(COVER.ID.asc())
                .fetchAsFlux()
                .map { DeleteCoverInformation(
                        it[COVER.ID],
                        it[COVER.URL].substringAfterLast("."),
                        ItemInformation(it[ITEM.ID], it[ITEM.TITLE]),
                        PodcastInformation(it[PODCAST.ID], it[PODCAST.TITLE])
                ) }

    }
}

data class CoverForCreation(val width: Int, val height: Int, val url: URI)
data class DeleteCoverInformation(
        val id: UUID,
        val extension: String,
        val item: ItemInformation,
        val podcast: PodcastInformation
) {
    data class ItemInformation(val id: UUID, val title: String)
    data class PodcastInformation(val id: UUID, val title: String)
}
