package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.database.Tables.COVER
import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.database.Tables.PODCAST
import com.github.davinkevin.podcastserver.database.Tables.WATCH_LIST_ITEMS
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.Status.FINISH
import com.github.davinkevin.podcastserver.extension.repository.executeAsyncAsMono
import com.github.davinkevin.podcastserver.extension.repository.fetchOneAsMono
import com.github.davinkevin.podcastserver.extension.repository.toUTC
import org.jooq.DSLContext
import org.jooq.impl.DSL.value
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.*

/**
 * Created by kevin on 2019-02-03
 */
@Repository
class ItemRepositoryV2(private val query: DSLContext) {

    fun findById(id: UUID) = Mono.defer {
        query
                .select(ITEM.ID, ITEM.TITLE, ITEM.URL,
                        ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE,
                        ITEM.DESCRIPTION, ITEM.MIME_TYPE, ITEM.LENGTH, ITEM.FILE_NAME, ITEM.STATUS,

                        PODCAST.ID, PODCAST.TITLE, PODCAST.URL,
                        COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT
                )
                .from(
                        ITEM.innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID))
                                .innerJoin(COVER).on(ITEM.COVER_ID.eq(COVER.ID))
                )
                .where(ITEM.ID.eq(id))
                .fetchOneAsMono()
                .map {
                    val c = CoverForItem(it[COVER.ID], it[COVER.URL], it[COVER.WIDTH], it[COVER.HEIGHT])
                    val p = PodcastForItem(it[PODCAST.ID], it[PODCAST.TITLE], it[PODCAST.URL])
                    Item(
                            it[ITEM.ID], it[ITEM.TITLE], it[ITEM.URL],
                            it[ITEM.PUB_DATE].toUTC(), it[ITEM.DOWNLOAD_DATE].toUTC(), it[ITEM.CREATION_DATE].toUTC(),
                            it[ITEM.DESCRIPTION], it[ITEM.MIME_TYPE], it[ITEM.LENGTH], it[ITEM.FILE_NAME], Status.of(it[ITEM.STATUS]),
                            p, c
                    )
                }

    }

    fun findAllToDelete(date: OffsetDateTime) = Flux.defer {
        query
                .select(ITEM.ID, ITEM.FILE_NAME, PODCAST.TITLE)
                .from(ITEM.innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID)))
                .where(ITEM.DOWNLOAD_DATE.lessOrEqual(Timestamp.valueOf(date.toLocalDateTime())))
                .and(ITEM.STATUS.eq(FINISH.toString()))
                .and(PODCAST.HAS_TO_BE_DELETED.isTrue)
                .and(ITEM.ID.notIn(query.select(WATCH_LIST_ITEMS.ITEMS_ID).from(WATCH_LIST_ITEMS)))
                .fetch { DeleteItemInformation(it[ITEM.ID], it[ITEM.FILE_NAME], it[PODCAST.TITLE]) }
                .toFlux()
    }

    fun deleteById(items: Collection<UUID>) = Mono.defer {
        query
                .deleteFrom(ITEM)
                .where(ITEM.ID.`in`(items))
                .executeAsyncAsMono()
                .then()
    }

    fun updateAsDeleted(items: Collection<UUID>) = Mono.defer {
        query
                .update(ITEM)
                .set(ITEM.STATUS, Status.DELETED.toString())
                .set(ITEM.FILE_NAME, value<String>(null))
                .where(ITEM.ID.`in`(items))
                .executeAsyncAsMono()
                .then()
    }

    fun hasToBeDeleted(id: UUID): Mono<Boolean> = Mono.defer {
        query
                .select(PODCAST.HAS_TO_BE_DELETED)
                .from(ITEM.innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID)))
                .where(ITEM.ID.eq(id))
                .fetchOneAsMono()
                .map { it[PODCAST.HAS_TO_BE_DELETED] }
    }

    fun resetById(id: UUID): Mono<Item> = Mono.defer {
        query
                .update(ITEM)
                .set(ITEM.STATUS, Status.NOT_DOWNLOADED.toString())
                .set(ITEM.DOWNLOAD_DATE, value<Timestamp>(null))
                .set(ITEM.FILE_NAME, value<String>(null))
                .set(ITEM.NUMBER_OF_FAIL, 0)
                .where(ITEM.ID.eq(id))
                .executeAsyncAsMono()
                .flatMap { findById(id) }
    }
}
