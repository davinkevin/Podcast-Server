package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.database.Tables.COVER
import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.database.Tables.PODCAST
import com.github.davinkevin.podcastserver.database.Tables.PODCAST_TAGS
import com.github.davinkevin.podcastserver.database.Tables.TAG
import com.github.davinkevin.podcastserver.database.Tables.WATCH_LIST_ITEMS
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.Status.FINISH
import com.github.davinkevin.podcastserver.extension.repository.executeAsyncAsMono
import com.github.davinkevin.podcastserver.extension.repository.fetchAsFlux
import com.github.davinkevin.podcastserver.extension.repository.fetchOneAsMono
import com.github.davinkevin.podcastserver.extension.repository.toUTC
import org.jooq.DSLContext
import org.jooq.Record18
import org.jooq.impl.DSL.and
import org.jooq.impl.DSL.countDistinct
import org.jooq.impl.DSL.trueCondition
import org.jooq.impl.DSL.value
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.util.function.component1
import reactor.util.function.component2
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
                .map(::toItem)
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

    fun search(tags: List<String>, statuses: List<Status>, page: ItemPageRequest): Mono<PageItem> = Mono.defer {
        query
                .select(TAG.ID)
                .from(TAG)
                .where(TAG.NAME.`in`(tags))
                .fetchAsFlux()
                .map { (v) -> v }
                .collectList()
                .flatMap { tagIds ->
                    val tables = ITEM
                            .innerJoin(COVER).on(ITEM.COVER_ID.eq(COVER.ID))
                            .innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID))

                    val statusesCondition = if (statuses.isEmpty()) trueCondition() else ITEM.STATUS.`in`(statuses)
                    val tagsCondition = if (tagIds.isEmpty()) trueCondition() else {
                        val multipleTagsCondition = tagIds.map {
                            value(it).`in`(query
                                    .select(PODCAST_TAGS.TAGS_ID)
                                    .from(PODCAST_TAGS)
                                    .where(PODCAST.ID.eq(PODCAST_TAGS.PODCASTS_ID))
                            ) }
                        and(multipleTagsCondition)
                    }

                    val content = query
                            .selectDistinct(
                                    ITEM.ID, ITEM.TITLE, ITEM.URL,
                                    ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE,
                                    ITEM.DESCRIPTION, ITEM.MIME_TYPE, ITEM.LENGTH, ITEM.FILE_NAME, ITEM.STATUS,

                                    PODCAST.ID, PODCAST.TITLE, PODCAST.URL,
                                    COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT
                            )
                            .from(tables)
                            .where(statusesCondition.and(tagsCondition))
                            .orderBy(page.sort.toOrderBy())
                            .limit((page.size * page.page), page.size)
                            .fetchAsFlux()
                            .map(::toItem)
                            .collectList()

                    val totalElements = query
                            .select(countDistinct(ITEM.ID))
                            .from(tables)
                            .where(statusesCondition.and(tagsCondition))
                            .fetchOneAsMono()
                            .map { (v) -> v }


                    Mono.zip(content, totalElements)
                            .map { (content, totalElements) -> PageItem.of(content, totalElements, page) }
                }


    }
}

private fun toItem(it: Record18<UUID, String, String, Timestamp, Timestamp, Timestamp, String, String, Long, String, String, UUID, String, String, UUID, String, Int, Int>): Item {
    val c = CoverForItem(it[COVER.ID], it[COVER.URL], it[COVER.WIDTH], it[COVER.HEIGHT])
    val p = PodcastForItem(it[PODCAST.ID], it[PODCAST.TITLE], it[PODCAST.URL])
    return Item(
            it[ITEM.ID], it[ITEM.TITLE], it[ITEM.URL],
            it[ITEM.PUB_DATE].toUTC(), it[ITEM.DOWNLOAD_DATE].toUTC(), it[ITEM.CREATION_DATE].toUTC(),
            it[ITEM.DESCRIPTION], it[ITEM.MIME_TYPE], it[ITEM.LENGTH], it[ITEM.FILE_NAME], Status.of(it[ITEM.STATUS]),
            p, c
    )
}

private fun ItemSort.toOrderBy() = when(field) {
    "pubDate" -> ITEM.PUB_DATE
    "downloadDate" -> ITEM.DOWNLOAD_DATE
    else -> ITEM.PUB_DATE
}.let { when(direction.toUpperCase()) {
    "ASC" -> it.asc()
    "DESC" -> it.desc()
    else -> it.desc()
} }

