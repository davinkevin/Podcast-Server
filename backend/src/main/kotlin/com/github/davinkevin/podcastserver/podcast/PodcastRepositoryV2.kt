package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.database.Tables.COVER
import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.database.Tables.PODCAST
import com.github.davinkevin.podcastserver.database.Tables.PODCAST_TAGS
import com.github.davinkevin.podcastserver.database.Tables.TAG
import com.github.davinkevin.podcastserver.database.tables.records.ItemRecord
import com.github.davinkevin.podcastserver.extension.repository.executeAsyncAsMono
import com.github.davinkevin.podcastserver.extension.repository.fetchAsFlux
import com.github.davinkevin.podcastserver.extension.repository.fetchOneAsMono
import com.github.davinkevin.podcastserver.extension.repository.toUTC
import com.github.davinkevin.podcastserver.tag.Tag
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import reactor.util.function.component1
import reactor.util.function.component2
import java.net.URI
import java.sql.Timestamp
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

class PodcastRepositoryV2(private val query: DSLContext) {

    fun findById(id: UUID) = Mono.zip(
            Mono.defer { query
                    .select(
                            PODCAST.ID, PODCAST.TITLE, PODCAST.DESCRIPTION, PODCAST.URL,
                            PODCAST.HAS_TO_BE_DELETED, PODCAST.LAST_UPDATE,
                            PODCAST.TYPE,
                            COVER.ID, COVER.URL, COVER.HEIGHT, COVER.WIDTH
                    )
                    .from(PODCAST.innerJoin(COVER).on(PODCAST.COVER_ID.eq(COVER.ID)))
                    .where(PODCAST.ID.eq(id))
                    .fetchOneAsMono()
            },
            findTagsByPodcastId(id)
    )
            .map { (p, tags) ->
                val c = CoverForPodcast(p[COVER.ID], URI(p[COVER.URL]), p[COVER.WIDTH], p[COVER.HEIGHT])

                Podcast (
                        p[PODCAST.ID], p[PODCAST.TITLE], p[PODCAST.DESCRIPTION], p[PODCAST.URL],
                        p[PODCAST.HAS_TO_BE_DELETED], p[PODCAST.LAST_UPDATE].toUTC(),
                        p[PODCAST.TYPE],

                        tags,
                        c
                )
            }


    private fun findTagsByPodcastId(id: UUID): Mono<Collection<Tag>> = Mono.defer {
        query.select(TAG.ID, TAG.NAME)
                .from(TAG.innerJoin(PODCAST_TAGS).on(TAG.ID.eq(PODCAST_TAGS.TAGS_ID)))
                .where(PODCAST_TAGS.PODCASTS_ID.eq(id))
                .fetchAsFlux()
                .map { Tag(it[TAG.ID], it[TAG.NAME]) }
                .collectList()
    }

    fun findAll(): Flux<Podcast> = Flux.defer {
        query
                .select(
                        PODCAST.ID, PODCAST.TITLE, PODCAST.URL,
                        PODCAST.HAS_TO_BE_DELETED, PODCAST.LAST_UPDATE,
                        PODCAST.TYPE, PODCAST.DESCRIPTION,

                        COVER.ID, COVER.URL, COVER.HEIGHT, COVER.WIDTH,

                        TAG.ID, TAG.NAME
                )
                .from(
                        PODCAST
                                .innerJoin(COVER).on(PODCAST.COVER_ID.eq(COVER.ID))
                                .leftJoin(PODCAST_TAGS).on(PODCAST_TAGS.PODCASTS_ID.eq(PODCAST.ID))
                                .leftJoin(TAG).on(PODCAST_TAGS.TAGS_ID.eq(TAG.ID))
                )
                .fetchAsFlux()
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
                                        p[PODCAST.ID], p[PODCAST.TITLE], p[PODCAST.DESCRIPTION], p[PODCAST.URL],
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

        query
                .select(DSL.count(), date)
                .from(ITEM)
                .where(ITEM.PODCAST_ID.eq(pid))
                .and(field.isNotNull)
                .and(DSL.dateDiff(DSL.currentDate(), DSL.date(field)).lessThan(numberOfDays.toInt()))
                .groupBy(date)
                .orderBy(date.desc())
                .fetchAsFlux()
                .map { NumberOfItemByDateWrapper(it.component2().toLocalDateTime().toLocalDate(), it.component1()) }
    }

    fun save(title: String, url: String, hasToBeDeleted: Boolean, type: String, tags: Collection<Tag>, cover: Cover) = Mono.defer {
        val id = UUID.randomUUID()

        val insertPodcast = query
                .insertInto(PODCAST, PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.HAS_TO_BE_DELETED, PODCAST.TYPE, PODCAST.COVER_ID)
                .values(id, title, url, hasToBeDeleted, type, cover.id)
                .executeAsyncAsMono()

        val linkToTags = if (tags.isEmpty()) Mono.empty() else {
            query.insertInto(PODCAST_TAGS, PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID )
                    .apply { tags.forEach { values(id, it.id) } }
                    .executeAsyncAsMono()
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
                .executeAsyncAsMono()

        val deleteAllTags = query
                .delete(PODCAST_TAGS)
                .where(PODCAST_TAGS.PODCASTS_ID.eq(id))
                .executeAsyncAsMono()

        val insertNewTags = if (tags.isEmpty()) Mono.empty()
                    else query
                            .insertInto(PODCAST_TAGS, PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                            .apply { tags.forEach { values(id, it.id) } }
                            .executeAsyncAsMono()

        update
                .then(deleteAllTags)
                .then(insertNewTags)
                .then(findById(id))
    }
}
