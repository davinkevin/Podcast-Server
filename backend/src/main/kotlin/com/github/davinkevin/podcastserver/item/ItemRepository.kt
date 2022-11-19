package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.database.tables.records.CoverRecord
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.Status.*
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.DSL.*
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.*

/**
 * Created by kevin on 2019-02-03
 */
class ItemRepository(private val query: DSLContext) {

    private val log = LoggerFactory.getLogger(ItemRepository::class.java)

    fun findById(id: UUID) = Mono.defer { findById(listOf(id)).toMono() }

    fun findById(ids: List<UUID>) = Flux.defer {
        Flux.from(query
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
            .where(ITEM.ID.`in`(ids))
        )
            .map(::toItem)
    }


    fun findAllToDelete(date: OffsetDateTime) = Flux.defer {
        Flux.from(query
            .select(ITEM.ID, ITEM.FILE_NAME, PODCAST.TITLE)
            .from(ITEM.innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID)))
            .where(ITEM.DOWNLOAD_DATE.lessOrEqual(date))
            .and(ITEM.STATUS.eq(FINISH))
            .and(PODCAST.HAS_TO_BE_DELETED.isTrue)
            .and(ITEM.ID.notIn(query.select(WATCH_LIST_ITEMS.ITEMS_ID).from(WATCH_LIST_ITEMS)))
        )
            .map { DeleteItemRequest(it[ITEM.ID], it[ITEM.FILE_NAME], it[PODCAST.TITLE]) }
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
            .map { DeleteItemRequest(it[ITEM.ID], it[ITEM.FILE_NAME], it[PODCAST.TITLE]) }

        removeFromPlaylist.then(delete)
    }

    fun updateAsDeleted(items: Collection<UUID>): Mono<Void> = Mono.defer {
        query
            .update(ITEM)
            .set(ITEM.STATUS, DELETED)
            .set(ITEM.FILE_NAME, null as Path?)
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
            .set(ITEM.DOWNLOAD_DATE, null as OffsetDateTime?)
            .set(ITEM.FILE_NAME, null as Path?)
            .set(ITEM.NUMBER_OF_FAIL, 0)
            .where(ITEM.ID.eq(id))
            .toMono()
            .flatMap { findById(id) }
    }

    fun search(q: String, tags: List<String>, status: List<Status>, page: ItemPageRequest, podcastId: UUID?): Mono<PageItem> = Mono.defer {

        val statusesCondition = if (status.isEmpty()) noCondition() else ITEM.STATUS.`in`(status)

        val tagsCondition = if (tags.isEmpty()) noCondition() else {
            tags
                .map {
                    value(it).`in`(query
                        .select(TAG.NAME)
                        .from(PODCAST_TAGS.innerJoin(TAG).on(TAG.ID.eq(PODCAST_TAGS.TAGS_ID)))
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

    fun create(item: ItemForCreation): Mono<Item> = Mono.defer {
        create(listOf(item)).toMono()
    }

    fun create(items: List<ItemForCreation>): Flux<Item> = Flux.defer {
        val itemsWithIds = items.map(::ItemForCreationWithId)

        val queries = itemsWithIds.map { v ->
            val id = v.id
            val item = v.itemForCreation

            val coverId = value(UUID.randomUUID()).cast(UUID::class.java)
            val itemNotExist =
                notExists(
                    selectOne()
                    .from(ITEM)
                    .where(ITEM.URL.eq(item.url))
                        .and(ITEM.PODCAST_ID.eq(item.podcastId)))

            val selectFromItem = select(
                coverId,
                value(item.cover?.height),
                value(item.cover?.width),
                value(item.cover?.url?.toASCIIString())
            )
                .where(itemNotExist)
                .and(
                    value(item.cover?.height).isNotNull
                    .and(value(item.cover?.width).isNotNull)
                    .and(value(item.cover?.url).isNotNull)
                )

            val selectFromPodcast = select(coverId, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                .from(PODCAST.innerJoin(COVER).on(PODCAST.COVER_ID.eq(COVER.ID)))
                .where(itemNotExist)

            val coverCreation: CommonTableExpression<CoverRecord> = name("COVER_CREATION").`as`(
                insertInto(COVER, COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL).select(
                    selectFromItem.unionAll(selectFromPodcast)
                )
                    .onConflictDoNothing()
                    .returning(COVER.ID)
            )

            query.with(coverCreation)
                .insertInto(
                    ITEM,
                    ITEM.ID, ITEM.TITLE, ITEM.URL,
                    ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE,
                    ITEM.DESCRIPTION, ITEM.MIME_TYPE, ITEM.LENGTH, ITEM.FILE_NAME, ITEM.STATUS,
                    ITEM.PODCAST_ID, ITEM.COVER_ID
                )
                .select(
                    select(
                        value(id),
                        value(item.title),
                        value(item.url),
                        value(item.pubDate),
                        value(item.downloadDate),
                        value(item.creationDate),
                        value(item.description),
                        value(item.mimeType),
                        value(item.length),
                        value(item.fileName),
                        value(item.status),
                        value(item.podcastId),
                        coverCreation.field(COVER.ID)
                    )
                        .from(coverCreation)
                )
                .onConflictDoNothing()
        }

        Flux.from(query.batch(queries))
            .index()
            .filter { (_, isCreated) -> isCreated >= 1 }
            .map { (idx, _) -> itemsWithIds[idx.toInt()].id }
            .collectList()
            .flatMapMany { findById(it) }
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

private fun toItem(it: Record18<UUID, String, String, OffsetDateTime, OffsetDateTime, OffsetDateTime, String, String, Long, Path, Status, UUID, String, String, UUID, String, Int, Int>): Item {
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

private data class ItemForCreationWithId(
    val itemForCreation: ItemForCreation
) {
    val id: UUID = UUID.randomUUID()
}
