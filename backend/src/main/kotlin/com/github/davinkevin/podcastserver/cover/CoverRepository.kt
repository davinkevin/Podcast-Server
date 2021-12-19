package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.cover.DeleteCoverInformation.Item
import com.github.davinkevin.podcastserver.cover.DeleteCoverInformation.Podcast
import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.database.Tables.PODCAST
import com.github.davinkevin.podcastserver.database.tables.Cover.COVER
import org.jooq.DSLContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

class CoverRepository(private val query: DSLContext) {

    fun save(cover: CoverForCreation): Mono<Cover> = Mono.defer{
        val id = UUID.randomUUID()
        query.insertInto(COVER)
                .set(COVER.ID, id)
                .set(COVER.WIDTH, cover.width)
                .set(COVER.HEIGHT, cover.height)
                .set(COVER.URL, cover.url.toASCIIString())
                .toMono()
                .map { Cover(id, cover.url, cover.height, cover.width) }
    }
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())

    fun findCoverOlderThan(date: OffsetDateTime): Flux<DeleteCoverInformation> = Flux.defer {
        Flux.from(
                        query
                                .select(
                                        PODCAST.ID, PODCAST.TITLE,
                                        ITEM.ID, ITEM.TITLE,
                                        COVER.ID, COVER.URL
                                )
                                .from(COVER
                                        .innerJoin(ITEM).on(COVER.ID.eq(ITEM.COVER_ID))
                                        .innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID))
                                )
                                .where(ITEM.CREATION_DATE.lessOrEqual(date))
                                .orderBy(COVER.ID.asc())
        )
                .subscribeOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.parallel())
                .map { (podcastId, podcastTitle, itemId, itemTitle, coverId, coverUrl) ->
                    DeleteCoverInformation(
                            id = coverId,
                            extension = coverUrl.substringAfterLast("."),
                            item = Item(itemId, itemTitle),
                            podcast = Podcast(podcastId, podcastTitle)
                    )
                }

    }
}

data class CoverForCreation(val width: Int, val height: Int, val url: URI)
data class DeleteCoverInformation(val id: UUID, val extension: String, val item: Item, val podcast: Podcast) {
    data class Item(val id: UUID, val title: String)
    data class Podcast(val id: UUID, val title: String)
}
