package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.business.stats.NumberOfItemByDateWrapper
import com.github.davinkevin.podcastserver.business.stats.StatsPodcastType
import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.database.tables.records.ItemRecord
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.Status.FINISH
import com.github.davinkevin.podcastserver.extension.repository.executeAsyncAsMono
import com.github.davinkevin.podcastserver.extension.repository.fetchOneAsMono
import com.github.davinkevin.podcastserver.extension.repository.toUTC
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL.*
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import java.sql.Timestamp
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZonedDateTime.now
import java.util.*

/**
 * Created by kevin on 2019-02-03
 */
@Repository
class ItemRepositoryV2(private val query: DSLContext) {

    fun findStatOfCreationDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.CREATION_DATE)
    fun findStatOfPubDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.PUB_DATE)
    fun findStatOfDownloadDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.DOWNLOAD_DATE)

    fun allStatsByTypeAndCreationDate(numberOfMonth: Int) = allStatsByTypeAndField(numberOfMonth, ITEM.CREATION_DATE)
    fun allStatsByTypeAndPubDate(numberOfMonth: Int) = allStatsByTypeAndField(numberOfMonth, ITEM.PUB_DATE)
    fun allStatsByTypeAndDownloadDate(numberOfMonth: Int) = allStatsByTypeAndField(numberOfMonth, ITEM.DOWNLOAD_DATE)

    private fun findStatOfOnField(pid: UUID, month: Int, field: TableField<ItemRecord, Timestamp>): Set<NumberOfItemByDateWrapper> {
        val date = trunc(field)
        val startDate = now().minusMonths(month.toLong())
        val numberOfDays = Duration.between(startDate, now()).toDays()

        return query
                .select(count(), date)
                .from(ITEM)
                .where(ITEM.PODCAST_ID.eq(pid))
                .and(field.isNotNull)
                .and(dateDiff(currentDate(), date(field)).lessThan(numberOfDays.toInt()))
                .groupBy(date)
                .orderBy(date.desc())
                .fetch { NumberOfItemByDateWrapper(it.component2().toLocalDateTime().toLocalDate(), it.component1()) }
                .toSet()
    }

    private fun allStatsByTypeAndField(month: Int, field: TableField<ItemRecord, Timestamp>): List<StatsPodcastType> {
        val date = trunc(field)
        val startDate = now().minusMonths(month.toLong())
        val numberOfDays = Duration.between(startDate, now()).toDays()

        return query
                .select(PODCAST.TYPE, count(), date)
                .from(ITEM.innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID)))
                .where(field.isNotNull)
                .and(dateDiff(currentDate(), date(field)).lessThan(numberOfDays.toInt()))
                .groupBy(PODCAST.TYPE, date)
                .orderBy(date.desc())
                .groupBy { it[PODCAST.TYPE] }
                .map {
                    StatsPodcastType(
                            type = it.key,
                            values = it.value
                                    .map { (_, number, date) -> NumberOfItemByDateWrapper(date.toLocalDateTime().toLocalDate(), number) }
                                    .toSet()
                                    .toVΛVΓ()
                    )
                }
    }

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
}
