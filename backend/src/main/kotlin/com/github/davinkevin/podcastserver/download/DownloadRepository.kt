package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.repository.executeAsyncAsMono
import com.github.davinkevin.podcastserver.extension.repository.fetchAsFlux
import com.github.davinkevin.podcastserver.extension.repository.fetchOneAsMono
import com.github.davinkevin.podcastserver.extension.repository.toTimestamp
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem.*
import org.jooq.DSLContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.*

/**
 * Created by kevin on 22/09/2019
 */
class DownloadRepository(private val query: DSLContext) {

    fun findAllToDownload(fromDate: OffsetDateTime, withMaxNumberOfTry: Int) = Flux.defer {
        query
                .select(
                        ITEM.ID, ITEM.TITLE, ITEM.STATUS, ITEM.URL, ITEM.NUMBER_OF_FAIL,
                        PODCAST.ID, PODCAST.TITLE,
                        COVER.ID, COVER.URL
                )
                .from(ITEM
                        .innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID))
                        .innerJoin(COVER).on(ITEM.COVER_ID.eq(COVER.ID))
                )
                .where(
                        ITEM.PUB_DATE.gt(Timestamp.valueOf(fromDate.toLocalDateTime()))
                                .and(
                                        ITEM.STATUS.isNull.or(ITEM.STATUS.eq(Status.NOT_DOWNLOADED.toString()))
                                )
                                .and(ITEM.NUMBER_OF_FAIL.isNull.or(ITEM.NUMBER_OF_FAIL.lt(withMaxNumberOfTry)))
                )
                .fetchAsFlux()
                .map { DownloadingItem(
                        it[ITEM.ID], it[ITEM.TITLE], Status.of(it[ITEM.STATUS]), URI(it[ITEM.URL]), it[ITEM.NUMBER_OF_FAIL] ?: 0, 0,
                        Podcast(it[PODCAST.ID], it[PODCAST.TITLE]),
                        Cover(it[COVER.ID], URI(it[COVER.URL]))
                ) }

    }

    fun findDownloadingItemById(id: UUID) = Mono.defer {
        query
                .select(
                        ITEM.ID, ITEM.TITLE, ITEM.STATUS, ITEM.URL, ITEM.NUMBER_OF_FAIL,
                        PODCAST.ID, PODCAST.TITLE,
                        COVER.ID, COVER.URL
                )
                .from(ITEM
                        .innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID))
                        .innerJoin(COVER).on(ITEM.COVER_ID.eq(COVER.ID))
                )
                .where(ITEM.ID.eq(id))
                .fetchOneAsMono()
                .map { DownloadingItem(
                        it[ITEM.ID], it[ITEM.TITLE], Status.of(it[ITEM.STATUS]), URI(it[ITEM.URL]), it[ITEM.NUMBER_OF_FAIL] ?: 0,0,
                        Podcast(it[PODCAST.ID], it[PODCAST.TITLE]),
                        Cover(it[COVER.ID], URI(it[COVER.URL]))
                ) }
    }

    fun stopItem(id: UUID) = Mono.defer {
        query
                .update(ITEM)
                .set(ITEM.STATUS, Status.STOPPED.toString())
                .where(ITEM.ID.eq(id))
                .executeAsyncAsMono()
    }

    fun updateDownloadItem(item: DownloadingItem): Mono<Int> = Mono.defer {
        query
                .update(ITEM)
                .set(ITEM.STATUS, item.status.toString())
                .set(ITEM.NUMBER_OF_FAIL, item.numberOfFail)
                .where(ITEM.ID.eq(item.id))
                .executeAsyncAsMono()
    }

    fun finishDownload(id: UUID, length: Long, mimeType: String, fileName: String, downloadDate: OffsetDateTime): Mono<Int> = Mono.defer {
        query
                .update(ITEM)
                .set(ITEM.STATUS, Status.FINISH.toString())
                .set(ITEM.LENGTH, length)
                .set(ITEM.MIME_TYPE, mimeType)
                .set(ITEM.FILE_NAME, fileName)
                .set(ITEM.DOWNLOAD_DATE, downloadDate.toTimestamp())
                .where(ITEM.ID.eq(id))
                .executeAsyncAsMono()
    }


}
