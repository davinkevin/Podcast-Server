package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.Status.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record18
import org.jooq.SortField
import org.jooq.impl.DSL
import org.jooq.impl.DSL.*
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
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
                .subscribeOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.parallel())
                .map { toItem(it) }
    }

    fun findAllToDelete(date: OffsetDateTime): Flow<DeleteItemInformation> = Flux.defer {
        Flux.from(query
            .select(ITEM.ID, ITEM.FILE_NAME, PODCAST.TITLE)
            .from(ITEM.innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID)))
            .where(ITEM.DOWNLOAD_DATE.lessOrEqual(date))
            .and(ITEM.STATUS.eq(FINISH))
            .and(PODCAST.HAS_TO_BE_DELETED.isTrue)
            .and(ITEM.ID.notIn(query.select(WATCH_LIST_ITEMS.ITEMS_ID).from(WATCH_LIST_ITEMS)))
        )
            .map { DeleteItemInformation(it[ITEM.ID], it[ITEM.FILE_NAME], it[PODCAST.TITLE]) }
    }
        .subscribeOn(Schedulers.boundedElastic())
        .publishOn(Schedulers.parallel())
        .asFlow()

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
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())

    suspend fun updateAsDeleted(items: Collection<UUID>): Void? = Mono.defer {
        query
                .update(ITEM)
                .set(ITEM.STATUS, DELETED)
                .set(ITEM.FILE_NAME, null as String?)
                .where(ITEM.ID.`in`(items))
                .toMono()
                .then()
    }
        .subscribeOn(Schedulers.boundedElastic())
        .publishOn(Schedulers.parallel())
        .awaitFirst()

    fun hasToBeDeleted(id: UUID): Mono<Boolean> = Mono.defer {
        query
                .select(PODCAST.HAS_TO_BE_DELETED)
                .from(ITEM.innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID)))
                .where(ITEM.ID.eq(id))
                .toMono()
                .map { it[PODCAST.HAS_TO_BE_DELETED] }
    }
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())

    fun resetById(id: UUID): Mono<Item> = Mono.defer {
        query
                .update(ITEM)
                .set(ITEM.STATUS, NOT_DOWNLOADED)
                .set(ITEM.DOWNLOAD_DATE, null as OffsetDateTime?)
                .set(ITEM.FILE_NAME, null as String?)
                .set(ITEM.NUMBER_OF_FAIL, 0)
                .where(ITEM.ID.eq(id))
                .toMono()
                .flatMap { findById(id) }
    }
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())

    fun search(q: String, tags: List<String>, status: List<Status>, page: ItemPageRequest, podcastId: UUID?): Mono<PageItem> = Mono.defer {
        Flux.from(
                query
                        .select(TAG.ID)
                        .from(TAG)
                        .where(TAG.NAME.`in`(tags))
        )
                .map { (v) -> v }
                .collectList()
                .flatMap { tagIds ->

                    val statusesCondition = if (status.isEmpty()) noCondition() else ITEM.STATUS.`in`(status)
                    val tagsCondition = if (tagIds.isEmpty()) noCondition() else {
                        tagIds
                                .map {
                                    value(it).`in`(query
                                        .select(PODCAST_TAGS.TAGS_ID)
                                        .from(PODCAST_TAGS)
                                        .where(ITEM.PODCAST_ID.eq(PODCAST_TAGS.PODCASTS_ID)))
                                }
                                .reduce(DSL::and)
                    }
                    val queryCondition = if (q.isEmpty()) noCondition()
                    else or( ITEM.TITLE.containsIgnoreCase(q), ITEM.DESCRIPTION.containsIgnoreCase(q) )

                    val podcastCondition = if(podcastId == null) noCondition() else ITEM.PODCAST_ID.eq(podcastId)

                    val filterConditions = and(statusesCondition, tagsCondition, queryCondition, podcastCondition)

                    val fi = name("FILTERED_ITEMS").`as`(
                            select(
                                    ITEM.ID, ITEM.TITLE, ITEM.URL,
                                    ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE,
                                    ITEM.DESCRIPTION, ITEM.MIME_TYPE, ITEM.LENGTH, ITEM.FILE_NAME, ITEM.STATUS,

                                    ITEM.PODCAST_ID, ITEM.COVER_ID
                            )
                            .from(ITEM)
                            .where(filterConditions)
                            .orderBy(page.sort.toOrderBy(ITEM.DOWNLOAD_DATE, ITEM.PUB_DATE), ITEM.ID.asc())
                            .limit((page.size * page.page), page.size)
                    )

                    val content: Mono<List<Item>> = Flux.from(query
                            .with(fi)
                            .select(
                                    fi.field(ITEM.ID), fi.field(ITEM.TITLE), fi.field(ITEM.URL),
                                    fi.field(ITEM.PUB_DATE), fi.field(ITEM.DOWNLOAD_DATE), fi.field(ITEM.CREATION_DATE),
                                    fi.field(ITEM.DESCRIPTION), fi.field(ITEM.MIME_TYPE), fi.field(ITEM.LENGTH),
                                    fi.field(ITEM.FILE_NAME), fi.field(ITEM.STATUS),

                                    PODCAST.ID, PODCAST.TITLE, PODCAST.URL,
                                    COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT
                            )
                            .from(
                                    fi
                                            .innerJoin(COVER).on(fi.field(ITEM.COVER_ID)?.eq(COVER.ID))
                                            .innerJoin(PODCAST).on(fi.field(ITEM.PODCAST_ID)?.eq(PODCAST.ID))
                            )
                            .orderBy(page.sort.toOrderBy(fi.field(ITEM.DOWNLOAD_DATE)!!, fi.field(ITEM.PUB_DATE)!!), fi.field(ITEM.ID))
                    )
                            .map { (
                                           id, title, url,
                                           pubDate, downloadDate, creationDate,
                                           description, mimeType, length,
                                           fileName, status,

                                           podcastId, podcastTitle, podcastUrl,
                                           coverId, coverUrl, coverWidth, coverHeight
                                   ) -> Item(
                                        id, title, url,
                                        pubDate, downloadDate, creationDate,
                                        description, mimeType, length, fileName, status,

                                        Item.Podcast(podcastId, podcastTitle, podcastUrl),
                                        Item.Cover(coverId, URI(coverUrl), coverWidth, coverHeight)
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

            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())

    fun create(item: ItemForCreation): Mono<Item> = Mono.defer {
        query
                .select(ITEM.ID)
                .from(ITEM)
                .where(ITEM.URL.eq(item.url))
                .and(ITEM.PODCAST_ID.eq(item.podcastId))
                .toMono()
                .map { (id) -> id }
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
                            .set(ITEM.PUB_DATE, item.pubDate)
                            .set(ITEM.DOWNLOAD_DATE, item.downloadDate)
                            .set(ITEM.CREATION_DATE, item.creationDate)
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
    }

            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())

    fun resetItemWithDownloadingState(): Mono<Void> = Mono.defer {
        query
                .update(ITEM)
                .set(ITEM.STATUS, NOT_DOWNLOADED)
                .where(ITEM.STATUS.`in`(STARTED, PAUSED))
                .toMono()
                .then()
                .doOnTerminate { log.info("Reset of item with downloading state done") }
    }
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())

    fun findPlaylistsContainingItem(itemId: UUID): Flux<ItemPlaylist> = Flux.defer {
        Flux.from(
                query
                        .select(WATCH_LIST.ID, WATCH_LIST.NAME)
                        .from(WATCH_LIST
                                .innerJoin(WATCH_LIST_ITEMS)
                                .on(WATCH_LIST.ID.eq(WATCH_LIST_ITEMS.WATCH_LISTS_ID))
                        )
                        .where(WATCH_LIST_ITEMS.ITEMS_ID.eq(itemId))
                        .orderBy(WATCH_LIST.ID)
        )
                .map { (id, name) -> ItemPlaylist(id, name) }
    }
}

private fun toItem(it: Record18<UUID, String, String, OffsetDateTime, OffsetDateTime, OffsetDateTime, String, String, Long, String, Status, UUID, String, String, UUID, String, Int, Int>): Item {
    val c = Item.Cover(it[COVER.ID], URI(it[COVER.URL]), it[COVER.WIDTH], it[COVER.HEIGHT])
    val p = Item.Podcast(it[PODCAST.ID], it[PODCAST.TITLE], it[PODCAST.URL])
    return Item(
            it[ITEM.ID], it[ITEM.TITLE], it[ITEM.URL],
            it[ITEM.PUB_DATE], it[ITEM.DOWNLOAD_DATE], it[ITEM.CREATION_DATE],
            it[ITEM.DESCRIPTION], it[ITEM.MIME_TYPE], it[ITEM.LENGTH], it[ITEM.FILE_NAME], it[ITEM.STATUS],
            p, c
    )
}

private fun <T> ItemSort.toOrderBy(downloadDate: Field<T>, defaultField: Field<T>): SortField<T> {
    val field = if(field == "downloadDate" ) downloadDate else defaultField
    return if (direction.lowercase(Locale.getDefault()) == "asc") field.asc() else field.desc()
}
