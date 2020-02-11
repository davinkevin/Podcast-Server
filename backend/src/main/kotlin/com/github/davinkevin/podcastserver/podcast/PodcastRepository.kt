package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.database.tables.records.ItemRecord
import com.github.davinkevin.podcastserver.extension.repository.toTimestamp
import com.github.davinkevin.podcastserver.extension.repository.toUTC
import com.github.davinkevin.podcastserver.tag.Tag
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.sql.Timestamp
import java.time.Duration
import java.time.OffsetDateTime.now
import java.time.ZonedDateTime
import java.util.*

class PodcastRepository(private val query: DSLContext) {

    fun findById(id: UUID) = Mono.zip(
            Mono.defer { query
                    .select(
                            PODCAST.ID, PODCAST.TITLE, PODCAST.DESCRIPTION, PODCAST.SIGNATURE, PODCAST.URL,
                            PODCAST.HAS_TO_BE_DELETED, PODCAST.LAST_UPDATE,
                            PODCAST.TYPE,
                            COVER.ID, COVER.URL, COVER.HEIGHT, COVER.WIDTH
                    )
                    .from(PODCAST.innerJoin(COVER).on(PODCAST.COVER_ID.eq(COVER.ID)))
                    .where(PODCAST.ID.eq(id))
                    .toMono()
            },
            findTagsByPodcastId(id)
    )
            .map { (p, tags) ->
                val c = CoverForPodcast(p[COVER.ID], URI(p[COVER.URL]), p[COVER.WIDTH], p[COVER.HEIGHT])

                Podcast (
                        p[PODCAST.ID], p[PODCAST.TITLE], p[PODCAST.DESCRIPTION], p[PODCAST.SIGNATURE], p[PODCAST.URL],
                        p[PODCAST.HAS_TO_BE_DELETED], p[PODCAST.LAST_UPDATE].toUTC(),
                        p[PODCAST.TYPE],

                        tags,
                        c
                )
            }


    private fun findTagsByPodcastId(id: UUID): Mono<List<Tag>> = Mono.defer {

        Flux.from(
                query.select(TAG.ID, TAG.NAME)
                        .from(TAG.innerJoin(PODCAST_TAGS).on(TAG.ID.eq(PODCAST_TAGS.TAGS_ID)))
                        .where(PODCAST_TAGS.PODCASTS_ID.eq(id))
        )
                .map { Tag(it[TAG.ID], it[TAG.NAME]) }
                .collectList()
    }

    fun findAll(): Flux<Podcast> = Flux.defer {
        Flux.from(
                query
                        .select(
                                PODCAST.ID, PODCAST.TITLE, PODCAST.URL,
                                PODCAST.HAS_TO_BE_DELETED, PODCAST.LAST_UPDATE,
                                PODCAST.TYPE, PODCAST.DESCRIPTION, PODCAST.SIGNATURE,

                                COVER.ID, COVER.URL, COVER.HEIGHT, COVER.WIDTH,

                                TAG.ID, TAG.NAME
                        )
                        .from(
                                PODCAST
                                        .innerJoin(COVER).on(PODCAST.COVER_ID.eq(COVER.ID))
                                        .leftJoin(PODCAST_TAGS).on(PODCAST_TAGS.PODCASTS_ID.eq(PODCAST.ID))
                                        .leftJoin(TAG).on(PODCAST_TAGS.TAGS_ID.eq(TAG.ID))
                        )
        )
                .groupBy { it[PODCAST.ID] }
                .flatMap {

                    val group = it.cache()
                    val podcast = group.toMono()
                    val tags = group.filter { t -> t[TAG.ID] != null}
                            .map { v -> Tag(v[TAG.ID], v[TAG.NAME]) }
                            .collectList()

                    Mono.zip(podcast, tags)
                            .map { (p, t) ->
                                val c = CoverForPodcast(p[COVER.ID], URI(p[COVER.URL]), p[COVER.WIDTH], p[COVER.HEIGHT])

                                Podcast (
                                        p[PODCAST.ID], p[PODCAST.TITLE], p[PODCAST.DESCRIPTION], p[PODCAST.SIGNATURE] ,p[PODCAST.URL],
                                        p[PODCAST.HAS_TO_BE_DELETED], p[PODCAST.LAST_UPDATE].toUTC(),
                                        p[PODCAST.TYPE],

                                        t,
                                        c
                                )
                            }
                }
    }

    fun findStatByTypeAndCreationDate(numberOfMonth: Int) = findStatByTypeAndField(numberOfMonth, ITEM.CREATION_DATE)
    fun findStatByTypeAndPubDate(numberOfMonth: Int) = findStatByTypeAndField(numberOfMonth, ITEM.PUB_DATE)
    fun findStatByTypeAndDownloadDate(numberOfMonth: Int) = findStatByTypeAndField(numberOfMonth, ITEM.DOWNLOAD_DATE)

    private fun findStatByTypeAndField(month: Int, field: TableField<ItemRecord, Timestamp>): Flux<StatsPodcastType> {
        val date = DSL.trunc(field)
        val startDate = ZonedDateTime.now().minusMonths(month.toLong())
        val numberOfDays = Duration.between(startDate, ZonedDateTime.now()).toDays()

        return query
                .select(PODCAST.TYPE, DSL.count(), date)
                .from(ITEM.innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID)))
                .where(field.isNotNull)
                .and(DSL.dateDiff(DSL.currentDate(), DSL.date(field)).lessThan(numberOfDays.toInt()))
                .groupBy(PODCAST.TYPE, date)
                .orderBy(date.desc())
                .groupBy { it[PODCAST.TYPE] }
                .map { StatsPodcastType(
                        type = it.key,
                        values = it.value
                                .map { (_, number, date) -> NumberOfItemByDateWrapper(date.toLocalDateTime().toLocalDate(), number) }
                                .toSet()
                )
                }
                .toFlux()
    }

    fun findStatByPodcastIdAndPubDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.PUB_DATE)
    fun findStatByPodcastIdAndCreationDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.CREATION_DATE)
    fun findStatByPodcastIdAndDownloadDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.DOWNLOAD_DATE)

    private fun findStatOfOnField(pid: UUID, month: Int, field: TableField<ItemRecord, Timestamp>): Flux<NumberOfItemByDateWrapper> = Flux.defer {
        val date = DSL.trunc(field)
        val startDate = ZonedDateTime.now().minusMonths(month.toLong())
        val numberOfDays = Duration.between(startDate, ZonedDateTime.now()).toDays()

        Flux.from(
                query
                        .select(DSL.count(), date)
                        .from(ITEM)
                        .where(ITEM.PODCAST_ID.eq(pid))
                        .and(field.isNotNull)
                        .and(DSL.dateDiff(DSL.currentDate(), DSL.date(field)).lessThan(numberOfDays.toInt()))
                        .groupBy(date)
                        .orderBy(date.desc())
        )
                .map { NumberOfItemByDateWrapper(it.component2().toLocalDateTime().toLocalDate(), it.component1()) }
    }

    fun save(title: String, url: String?, hasToBeDeleted: Boolean, type: String, tags: Collection<Tag>, cover: Cover) = Mono.defer {
        val id = UUID.randomUUID()

        val insertPodcast = query
                .insertInto(PODCAST, PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.HAS_TO_BE_DELETED, PODCAST.TYPE, PODCAST.COVER_ID)
                .values(id, title, url, hasToBeDeleted, type, cover.id)
                .toMono()

        val linkToTags = if (tags.isEmpty()) Mono.empty() else {
            query.insertInto(PODCAST_TAGS, PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID )
                    .apply { tags.forEach { values(id, it.id) } }
                    .toMono()
        }

        insertPodcast
                .then(linkToTags)
                .then(findById(id))
    }

    fun update(id: UUID, title: String, url: String?, hasToBeDeleted: Boolean, tags: Collection<Tag>, cover: Cover): Mono<Podcast> = Mono.defer {

        val update = query
                .update(PODCAST)
                .set(PODCAST.TITLE, title)
                .set(PODCAST.URL, url)
                .set(PODCAST.HAS_TO_BE_DELETED, hasToBeDeleted)
                .set(PODCAST.COVER_ID, cover.id)
                .where(PODCAST.ID.eq(id))
                .toMono()

        val deleteAllTags = query
                .delete(PODCAST_TAGS)
                .where(PODCAST_TAGS.PODCASTS_ID.eq(id))
                .toMono()

        val insertNewTags = if (tags.isEmpty()) Mono.empty()
        else query
                .insertInto(PODCAST_TAGS, PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                .apply { tags.forEach { values(id, it.id) } }
                .toMono()

        update
                .then(deleteAllTags)
                .then(insertNewTags)
                .then(findById(id))
    }

    fun updateSignature(podcastId: UUID, newSignature: String): Mono<Void> = query
            .update(PODCAST)
            .set(PODCAST.SIGNATURE, newSignature)
            .where(PODCAST.ID.eq(podcastId))
            .toMono()
            .then()

    fun findCover(id: UUID): Mono<CoverForPodcast> = query
            .select(COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
            .from(PODCAST.innerJoin(COVER).on(PODCAST.COVER_ID.eq(COVER.ID)))
            .where(PODCAST.ID.eq(id))
            .toMono()
            .map { CoverForPodcast(
                    id = it[COVER.ID],
                    url = URI(it[COVER.URL]),
                    height = it[COVER.HEIGHT],
                    width = it[COVER.WIDTH]
            ) }

    fun updateLastUpdate(podcastId: UUID): Mono<Void> {
        return query
                .update(PODCAST)
                .set(PODCAST.LAST_UPDATE, now().toTimestamp())
                .where(PODCAST.ID.eq(podcastId))
                .toMono()
                .then()
    }

    fun deleteById(id: UUID): Mono<DeletePodcastInformation> {
        val removeItemFromPlaylist = query
                .delete(WATCH_LIST_ITEMS)
                .where(WATCH_LIST_ITEMS.ITEMS_ID.`in`(query
                        .select(ITEM.ID)
                        .from(ITEM)
                        .where(ITEM.PODCAST_ID.eq(id)))
                )
                .toMono()

        val removePodcastTags = query
                .delete(PODCAST_TAGS)
                .where(PODCAST_TAGS.PODCASTS_ID.eq(id))
                .toMono()

        val removeItems = query
                .delete(ITEM)
                .where(ITEM.PODCAST_ID.eq(id))
                .toMono()

        val deletePodcast = query
                .select(PODCAST.ID, PODCAST.TITLE, PODCAST.HAS_TO_BE_DELETED, PODCAST.COVER_ID)
                .from(PODCAST)
                .where(PODCAST.ID.eq(id))
                .toMono()
                .delayUntil {
                    val deletePodcast = query.delete(PODCAST).where(PODCAST.ID.eq(id)).toMono()
                    val deleteCover = query.delete(COVER).where(COVER.ID.eq(it[PODCAST.COVER_ID])).toMono()

                    deletePodcast.then(deleteCover)
                }
                .filter { it[PODCAST.HAS_TO_BE_DELETED] }
                .map { DeletePodcastInformation(it[PODCAST.ID], it[PODCAST.TITLE]) }

        return removeItemFromPlaylist
                .then(Mono.zip(removePodcastTags, removeItems))
                .then(deletePodcast)
    }
}
