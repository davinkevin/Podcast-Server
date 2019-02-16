package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.business.stats.NumberOfItemByDateWrapper
import com.github.davinkevin.podcastserver.business.stats.StatsPodcastType
import com.github.davinkevin.podcastserver.database.Tables
import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.database.tables.records.ItemRecord
import com.github.davinkevin.podcastserver.extension.repository.fetchAsFlux
import com.github.davinkevin.podcastserver.extension.repository.fetchOneAsMono
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import java.sql.Timestamp
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

class PodcastRepositoryV2(private val query: DSLContext) {

    fun findStatByPubDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.PUB_DATE)
    fun findStatByCreationDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.CREATION_DATE)
    fun findStatByDownloadDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.DOWNLOAD_DATE)

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

    fun findById(id: UUID) = Mono.defer {
        query
                .select(
                        PODCAST.ID, PODCAST.TITLE,
                        COVER.ID, COVER.URL, COVER.HEIGHT, COVER.WIDTH
                )
                .from(PODCAST.innerJoin(COVER).on(PODCAST.COVER_ID.eq(COVER.ID)))
                .where(PODCAST.ID.eq(id))
                .fetchOneAsMono()
                .map {
                    val c = CoverForPodcast(it[COVER.ID], it[COVER.URL], it[COVER.WIDTH], it[COVER.HEIGHT])

                    Podcast(it[PODCAST.ID], it[PODCAST.TITLE], c)
                }
    }

}
