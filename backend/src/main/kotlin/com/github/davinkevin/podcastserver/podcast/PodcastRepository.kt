package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.database.tables.records.ItemRecord
import com.github.davinkevin.podcastserver.tag.Tag
import org.jooq.DSLContext
import org.jooq.Record13
import org.jooq.Records.mapping
import org.jooq.TableField
import org.jooq.impl.DSL.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.sql.Date
import java.time.Duration
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now
import java.time.ZonedDateTime
import java.util.*

class PodcastRepository(private val query: DSLContext) {

    fun findById(id: UUID): Mono<Podcast> = query
        .select(
            PODCAST.ID, PODCAST.TITLE, PODCAST.DESCRIPTION, PODCAST.SIGNATURE, PODCAST.URL,
            PODCAST.HAS_TO_BE_DELETED, PODCAST.LAST_UPDATE,
            PODCAST.TYPE,

            PODCAST.cover().ID, PODCAST.cover().URL, PODCAST.cover().HEIGHT, PODCAST.cover().WIDTH,

            multiset(
                select(PODCAST_TAGS.tag().ID, PODCAST_TAGS.tag().NAME)
                    .from(PODCAST_TAGS)
                    .where(PODCAST_TAGS.PODCASTS_ID.eq(PODCAST.ID))
            )
                .convertFrom { r -> r.map(mapping(::Tag)) }
        )
        .from(PODCAST)
        .where(PODCAST.ID.eq(id))
        .toMono()
        .map(::toPodcast)

    fun findAll(): Flux<Podcast> = Flux.from(
            query
                .select(
                    PODCAST.ID, PODCAST.TITLE, PODCAST.DESCRIPTION, PODCAST.SIGNATURE, PODCAST.URL,
                    PODCAST.HAS_TO_BE_DELETED, PODCAST.LAST_UPDATE,
                    PODCAST.TYPE,

                    PODCAST.cover().ID, PODCAST.cover().URL, PODCAST.cover().HEIGHT, PODCAST.cover().WIDTH,

                    multiset(
                        select(PODCAST_TAGS.tag().ID, PODCAST_TAGS.tag().NAME)
                            .from(PODCAST_TAGS)
                            .where(PODCAST_TAGS.PODCASTS_ID.eq(PODCAST.ID))
                            .orderBy(PODCAST_TAGS.TAGS_ID)
                    )
                        .convertFrom { r -> r.map(mapping(::Tag)) },
                )
                .from(PODCAST)
                .orderBy(PODCAST.ID)
        )
            .map(::toPodcast)

    fun findStatByTypeAndCreationDate(numberOfMonth: Int) = findStatByTypeAndField(numberOfMonth, ITEM.CREATION_DATE)
    fun findStatByTypeAndPubDate(numberOfMonth: Int) = findStatByTypeAndField(numberOfMonth, ITEM.PUB_DATE)
    fun findStatByTypeAndDownloadDate(numberOfMonth: Int) = findStatByTypeAndField(numberOfMonth, ITEM.DOWNLOAD_DATE)

    private fun findStatByTypeAndField(month: Int, field: TableField<ItemRecord, OffsetDateTime>): Flux<StatsPodcastType> = Flux.defer {
        val date = trunc(field)
        val startDate = ZonedDateTime.now().minusMonths(month.toLong())
        val numberOfDays = Duration.between(startDate, ZonedDateTime.now()).toDays()

        Flux.from(query
            .select(PODCAST.TYPE, count(), date)
            .from(ITEM.innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID)))
            .where(field.isNotNull)
            .and(dateDiff(currentDate(), field.cast(Date::class.java)).lessThan(numberOfDays.toInt()))
            .groupBy(PODCAST.TYPE, date)
            .orderBy(date.desc())
        )
            .groupBy { it[PODCAST.TYPE] }
            .flatMap { group -> group
                .map { (_, number, date) -> NumberOfItemByDateWrapper(date.toLocalDate(), number) }
                .collectList()
                .map { StatsPodcastType(group.key(), it.toSet()) }
            }
    }

    fun findStatByPodcastIdAndPubDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.PUB_DATE)
    fun findStatByPodcastIdAndCreationDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.CREATION_DATE)
    fun findStatByPodcastIdAndDownloadDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.DOWNLOAD_DATE)

    private fun findStatOfOnField(pid: UUID, month: Int, field: TableField<ItemRecord, OffsetDateTime>): Flux<NumberOfItemByDateWrapper> = Flux.defer {
        val date = trunc(field)
        val startDate = ZonedDateTime.now().minusMonths(month.toLong())
        val numberOfDays = Duration.between(startDate, ZonedDateTime.now()).toDays()

        Flux.from(
            query
                .select(count(), date)
                .from(ITEM)
                .where(ITEM.PODCAST_ID.eq(pid))
                .and(field.isNotNull)
                .and(dateDiff(currentDate(), field.cast(Date::class.java)).lessThan(numberOfDays.toInt()))
                .groupBy(date)
                .orderBy(date.desc())
        )
            .map { (count, date) -> NumberOfItemByDateWrapper(date.toLocalDate(), count) }
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

    fun updateSignature(podcastId: UUID, newSignature: String): Mono<Void> = Mono.defer {
        query
            .update(PODCAST)
            .set(PODCAST.SIGNATURE, newSignature)
            .where(PODCAST.ID.eq(podcastId))
            .toMono()
    }
        .then()


    fun updateLastUpdate(podcastId: UUID): Mono<Void> = Mono.defer {
        query
            .update(PODCAST)
            .set(PODCAST.LAST_UPDATE, now())
            .where(PODCAST.ID.eq(podcastId))
            .toMono()
    }
        .then()

    fun deleteById(id: UUID): Mono<DeletePodcastRequest> = Mono.defer {
        val removeItemFromPlaylist = query
            .delete(PLAYLIST_ITEMS)
            .where(PLAYLIST_ITEMS.ITEMS_ID.`in`(query
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
            .map { DeletePodcastRequest(it[PODCAST.ID], it[PODCAST.TITLE]) }

        removeItemFromPlaylist
            .then(Mono.zip(removePodcastTags, removeItems))
            .then(deletePodcast)
    }
}

private fun toPodcast(r: Record13<UUID, String, String, String, String, Boolean, OffsetDateTime, String, UUID, String, Int, Int, List<Tag>>): Podcast {
    return Podcast(
        id = r[PODCAST.ID],
        title = r[PODCAST.TITLE],
        description = r[PODCAST.DESCRIPTION],
        signature = r[PODCAST.SIGNATURE],
        url = r[PODCAST.URL],
        hasToBeDeleted = r[PODCAST.HAS_TO_BE_DELETED],
        lastUpdate = r[PODCAST.LAST_UPDATE],
        type = r[PODCAST.TYPE],
        cover = Cover(
            id = r[PODCAST.cover().ID],
            url = URI(r[PODCAST.cover().URL]),
            height = r[PODCAST.cover().HEIGHT],
            width = r[PODCAST.cover().WIDTH],
        ),
        tags = r.component13(),
    )

}
