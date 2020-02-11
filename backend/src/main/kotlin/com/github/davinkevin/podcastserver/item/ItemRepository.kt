package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.Status.*
import com.github.davinkevin.podcastserver.extension.repository.toTimestamp
import com.github.davinkevin.podcastserver.extension.repository.toUTC
import org.jooq.DSLContext
import org.jooq.Record18
import org.jooq.impl.DSL.*
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.*

/**
 * Created by kevin on 2019-02-03
 */
class ItemRepository(private val query: DSLContext) {

    private val log = LoggerFactory.getLogger(ItemRepository::class.java)

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
                .toMono()
                .map(::toItem)
    }

    fun findAllToDelete(date: OffsetDateTime) = Flux.defer {
        query
                .select(ITEM.ID, ITEM.FILE_NAME, PODCAST.TITLE)
                .from(ITEM.innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID)))
                .where(ITEM.DOWNLOAD_DATE.lessOrEqual(Timestamp.valueOf(date.toLocalDateTime())))
                .and(ITEM.STATUS.eq(FINISH))
                .and(PODCAST.HAS_TO_BE_DELETED.isTrue)
                .and(ITEM.ID.notIn(query.select(WATCH_LIST_ITEMS.ITEMS_ID).from(WATCH_LIST_ITEMS)))
                .fetch { DeleteItemInformation(it[ITEM.ID], it[ITEM.FILE_NAME], it[PODCAST.TITLE]) }
                .toFlux()
    }

    fun deleteById(id: UUID) = Mono.defer {
        val removeFromPlaylist = query
                .delete(WATCH_LIST_ITEMS)
                .where(WATCH_LIST_ITEMS.ITEMS_ID.eq(id))
                .toMono()

        val delete = query
                .select(ITEM.ID, ITEM.FILE_NAME, ITEM.STATUS, PODCAST.TITLE, PODCAST.HAS_TO_BE_DELETED)
                .from(ITEM.innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID)))
                .where(ITEM.ID.eq(id))
                .toMono()
                .delayUntil { query.delete(ITEM).where(ITEM.ID.eq(id)).toMono() }
                .filter { it[PODCAST.HAS_TO_BE_DELETED] }
                .filter { it[ITEM.STATUS] == FINISH }
                .map { DeleteItemInformation(it[ITEM.ID], it[ITEM.FILE_NAME], it[PODCAST.TITLE]) }

        removeFromPlaylist.then(delete)
    }

    fun updateAsDeleted(items: Collection<UUID>) = Mono.defer {
        query
                .update(ITEM)
                .set(ITEM.STATUS, DELETED)
                .set(ITEM.FILE_NAME, value<String>(null))
                .where(ITEM.ID.`in`(items))
                .toMono()
                .then()
    }

    fun hasToBeDeleted(id: UUID): Mono<Boolean> = Mono.defer {
        query
                .select(PODCAST.HAS_TO_BE_DELETED)
                .from(ITEM.innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID)))
                .where(ITEM.ID.eq(id))
                .toMono()
                .map { it[PODCAST.HAS_TO_BE_DELETED] }
    }

    fun resetById(id: UUID): Mono<Item> = Mono.defer {
        query
                .update(ITEM)
                .set(ITEM.STATUS, NOT_DOWNLOADED)
                .set(ITEM.DOWNLOAD_DATE, value<Timestamp>(null))
                .set(ITEM.FILE_NAME, value<String>(null))
                .set(ITEM.NUMBER_OF_FAIL, 0)
                .where(ITEM.ID.eq(id))
                .toMono()
                .flatMap { findById(id) }
    }

    fun search(q: String?, tags: List<String>, statuses: List<Status>, page: ItemPageRequest, podcastId: UUID?): Mono<PageItem> = Mono.defer {

        Flux.from(
                query
                        .select(TAG.ID)
                        .from(TAG)
                        .where(TAG.NAME.`in`(tags))
        )
                .map { (v) -> v }
                .collectList()
                .flatMap { tagIds ->

                    val statusesCondition = if (statuses.isEmpty()) trueCondition() else ITEM.STATUS.`in`(statuses)
                    val tagsCondition = if (tagIds.isEmpty()) trueCondition() else {
                        val multipleTagsCondition = tagIds.map {
                            value(it).`in`(query
                                    .select(PODCAST_TAGS.TAGS_ID)
                                    .from(PODCAST_TAGS)
                                    .where(ITEM.PODCAST_ID.eq(PODCAST_TAGS.PODCASTS_ID))
                            ) }
                        and(multipleTagsCondition)
                    }
                    val queryCondition = if (q.isNullOrEmpty()) trueCondition() else {
                        or( ITEM.TITLE.containsIgnoreCase(q), ITEM.DESCRIPTION.containsIgnoreCase(q) )
                    }

                    val podcastCondition = if(podcastId == null) trueCondition() else {
                        ITEM.PODCAST_ID.eq(podcastId)
                    }

                    val filterConditions = and(statusesCondition, tagsCondition, queryCondition, podcastCondition)

                    val i = query
                            .select(
                                    ITEM.ID, ITEM.TITLE, ITEM.URL,
                                    ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE,
                                    ITEM.DESCRIPTION, ITEM.MIME_TYPE, ITEM.LENGTH, ITEM.FILE_NAME, ITEM.STATUS,

                                    ITEM.PODCAST_ID, ITEM.COVER_ID
                            )
                            .from(ITEM)
                            .where(filterConditions)
                            .orderBy(page.sort.toOrderBy(), ITEM.ID.asc())
                            .limit((page.size * page.page), page.size)
                            .asTable("FILTERED_ITEMS")

                    val itemId = i.field(ITEM.ID)
                    val itemTitle = i.field(ITEM.TITLE)
                    val itemURL = i.field(ITEM.URL)
                    val itemPubDate = i.field(ITEM.PUB_DATE)
                    val itemDownloadDate = i.field(ITEM.DOWNLOAD_DATE)
                    val itemCreationDate = i.field(ITEM.CREATION_DATE)
                    val itemDescription = i.field(ITEM.DESCRIPTION)
                    val itemMimeType = i.field(ITEM.MIME_TYPE)
                    val itemLength = i.field(ITEM.LENGTH)
                    val itemFileName = i.field(ITEM.FILE_NAME)
                    val itemStatus = i.field(ITEM.STATUS)

                    val content: Mono<List<Item>> = Flux.from(query
                            .select(
                                    itemId, itemTitle, itemURL,
                                    itemPubDate, itemDownloadDate, itemCreationDate,
                                    itemDescription, itemMimeType, itemLength,
                                    itemFileName, itemStatus,

                                    PODCAST.ID, PODCAST.TITLE, PODCAST.URL,
                                    COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT
                            )
                            .from(
                                    i
                                            .innerJoin(COVER).on(i.field(ITEM.COVER_ID).eq(COVER.ID))
                                            .innerJoin(PODCAST).on(i.field(ITEM.PODCAST_ID).eq(PODCAST.ID)))
                    )
                            .map {
                                val c = CoverForItem(it[COVER.ID], it[COVER.URL], it[COVER.WIDTH], it[COVER.HEIGHT])
                                val p = PodcastForItem(it[PODCAST.ID], it[PODCAST.TITLE], it[PODCAST.URL])
                                Item(
                                        it[itemId], it[itemTitle], it[itemURL],
                                        it[itemPubDate].toUTC(), it[itemDownloadDate].toUTC(), it[itemCreationDate].toUTC(),
                                        it[itemDescription], it[itemMimeType], it[itemLength], it[itemFileName], it[itemStatus],
                                        p, c
                                )
                            }
                            .collectList()

                    val totalElements = query
                            .select(countDistinct(ITEM.ID))
                            .from(ITEM)
                            .where(filterConditions)
                            .toMono()
                            .map { (v) -> v }


                    Mono.zip(content, totalElements)
                            .map { (content, totalElements) -> PageItem.of(content, totalElements, page) }
                }


    }

    fun create(item: ItemForCreation): Mono<Item> = query
            .select(ITEM.ID)
            .from(ITEM)
            .where(ITEM.URL.eq(item.url))
            .and(ITEM.PODCAST_ID.eq(item.podcastId))
            .toMono()
            .map { it[ITEM.ID] }
            .hasElement()
            .filter { it == false }
            .flatMap {
                val coverId = UUID.randomUUID()
                val insertCover = query.insertInto(COVER)
                        .set(COVER.ID, coverId)
                        .set(COVER.HEIGHT, item.cover.height)
                        .set(COVER.WIDTH, item.cover.width)
                        .set(COVER.URL, item.cover.url.toASCIIString())
                        .toMono()

                val id = UUID.randomUUID()
                val insertItem = query.insertInto(ITEM)
                        .set(ITEM.ID, id)
                        .set(ITEM.TITLE, item.title)
                        .set(ITEM.URL, item.url)
                        .set(ITEM.PUB_DATE, item.pubDate.toTimestamp())
                        .set(ITEM.DOWNLOAD_DATE, item.downloadDate.toTimestamp())
                        .set(ITEM.CREATION_DATE, item.creationDate.toTimestamp())
                        .set(ITEM.DESCRIPTION, item.description)
                        .set(ITEM.MIME_TYPE, item.mimeType)
                        .set(ITEM.LENGTH, item.length)
                        .set(ITEM.FILE_NAME, item.fileName)
                        .set(ITEM.STATUS, item.status)
                        .set(ITEM.PODCAST_ID, item.podcastId)
                        .set(ITEM.COVER_ID, coverId)
                        .toMono()


                insertCover
                        .then(insertItem)
                        .then(findById(id))
            }

    fun resetItemWithDownloadingState(): Mono<Void> = Mono.defer {
        query
                .update(ITEM)
                .set(ITEM.STATUS, NOT_DOWNLOADED)
                .where(ITEM.STATUS.`in`(STARTED, PAUSED))
                .toMono()
                .then()
                .doOnTerminate { log.info("Reset of item with downloading state done") }
    }

    fun findPlaylistsContainingItem(itemId: UUID): Flux<ItemPlaylist> {
        return Flux.from(query
                .select(WATCH_LIST.ID, WATCH_LIST.NAME)
                .from(WATCH_LIST
                        .innerJoin(WATCH_LIST_ITEMS)
                        .on(WATCH_LIST.ID.eq(WATCH_LIST_ITEMS.WATCH_LISTS_ID))
                )
                .where(WATCH_LIST_ITEMS.ITEMS_ID.eq(itemId))
                .orderBy(WATCH_LIST.ID)
        )
                .map { ItemPlaylist(it[WATCH_LIST.ID], it[WATCH_LIST.NAME]) }
    }
}

private fun toItem(it: Record18<UUID, String, String, Timestamp, Timestamp, Timestamp, String, String, Long, String, Status, UUID, String, String, UUID, String, Int, Int>): Item {
    val c = CoverForItem(it[COVER.ID], it[COVER.URL], it[COVER.WIDTH], it[COVER.HEIGHT])
    val p = PodcastForItem(it[PODCAST.ID], it[PODCAST.TITLE], it[PODCAST.URL])
    return Item(
            it[ITEM.ID], it[ITEM.TITLE], it[ITEM.URL],
            it[ITEM.PUB_DATE].toUTC(), it[ITEM.DOWNLOAD_DATE].toUTC(), it[ITEM.CREATION_DATE].toUTC(),
            it[ITEM.DESCRIPTION], it[ITEM.MIME_TYPE], it[ITEM.LENGTH], it[ITEM.FILE_NAME], it[ITEM.STATUS],
            p, c
    )
}

private fun ItemSort.toOrderBy() = when(field) {
    "downloadDate" -> ITEM.DOWNLOAD_DATE
    else -> ITEM.PUB_DATE
}.let { when(direction.toUpperCase()) {
    "ASC" -> it.asc()
    else -> it.desc()
} }

