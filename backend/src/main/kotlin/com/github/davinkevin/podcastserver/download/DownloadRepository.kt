package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.database.Tables.DOWNLOADING_ITEM
import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.database.enums.DownloadingState
import com.github.davinkevin.podcastserver.database.enums.ItemStatus
import com.github.davinkevin.podcastserver.database.enums.ItemStatus.*
import com.github.davinkevin.podcastserver.database.tables.Item
import com.github.davinkevin.podcastserver.entity.fromDb
import com.github.davinkevin.podcastserver.entity.toDb
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem.Cover
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem.Podcast
import org.jooq.DSLContext
import org.jooq.Record9
import org.jooq.impl.DSL.*
import java.net.URI
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.*

/**
 * Created by kevin on 22/09/2019
 */
class DownloadRepository(private val query: DSLContext) {

    fun initQueue(fromDate: OffsetDateTime, withMaxNumberOfTry: Int) {
        val positionInQueue = rowNumber()
            .over().orderBy(ITEM.PUB_DATE) +
          select(coalesce(max(DOWNLOADING_ITEM.POSITION), 0))
              .from(DOWNLOADING_ITEM).asField<Int>()

        query.insertInto(DOWNLOADING_ITEM, DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION)
            .select(
                select(ITEM.ID, positionInQueue)
                    .from(ITEM)
                    .where(ITEM.PUB_DATE.greaterThan(fromDate))
                    .and(ITEM.STATUS.eq(NOT_DOWNLOADED))
                    .and(ITEM.NUMBER_OF_FAIL.lt(withMaxNumberOfTry))
                    .and(ITEM.ID.notIn(select(DOWNLOADING_ITEM.ITEM_ID).from(DOWNLOADING_ITEM)))
                    .orderBy(ITEM.PUB_DATE.asc())
            )
            .execute()
    }

    fun addItemToQueue(id: UUID) {
        val positionInQueue = select(coalesce(max(DOWNLOADING_ITEM.POSITION), 0)).from(DOWNLOADING_ITEM).asField<Int>()

        query.insertInto(DOWNLOADING_ITEM, DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION)
            .select(
                select(ITEM.ID, positionInQueue + 1)
                    .from(ITEM)
                    .where(ITEM.ID.eq(id))
                    .and(ITEM.ID.notIn(select(DOWNLOADING_ITEM.ITEM_ID).from(DOWNLOADING_ITEM)))
            )
            .execute()
    }

    fun findAllToDownload(limit: Int): List<DownloadingItem> {
        val position = rowNumber().over().orderBy(DOWNLOADING_ITEM.POSITION)

        val snapshot = name("downloading_snapshot").`as`(
            select(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.STATE, position)
                .from(DOWNLOADING_ITEM)
                .limit(limit)
        )

        val item = DOWNLOADING_ITEM.item()

        val items = query.with(snapshot)
            .select(
                item.ID, item.TITLE, item.STATUS, item.URL, item.NUMBER_OF_FAIL,
                item.podcast().ID, item.podcast().TITLE,
                item.cover().ID, item.cover().URL
            )
            .from(
                snapshot
                    .innerJoin(DOWNLOADING_ITEM)
                    .on(snapshot.field(DOWNLOADING_ITEM.ITEM_ID)!!.eq(DOWNLOADING_ITEM.ITEM_ID))
            )
            .where(snapshot.field(DOWNLOADING_ITEM.STATE)!!.eq(DownloadingState.WAITING))
            .orderBy(snapshot.field(position))
            .fetch()

        return items.map(::toDownloadingItem)
    }

    fun findAllDownloading(): List<DownloadingItem> {
        val item = DOWNLOADING_ITEM.item()
        val items = query
            .select(
                item.ID, item.TITLE, item.STATUS, item.URL, item.NUMBER_OF_FAIL,
                item.podcast().ID, item.podcast().TITLE,
                item.cover().ID, item.cover().URL
            )
            .from(DOWNLOADING_ITEM)
            .where(DOWNLOADING_ITEM.STATE.eq(DownloadingState.DOWNLOADING))
            .orderBy(DOWNLOADING_ITEM.POSITION.asc())
            .fetch()

        return items.map(::toDownloadingItem)
    }

    fun findAllWaiting(): List<DownloadingItem> {
        val item = DOWNLOADING_ITEM.item()
        val items = query
            .select(
                item.ID, item.TITLE, item.STATUS, item.URL, item.NUMBER_OF_FAIL,
                item.podcast().ID, item.podcast().TITLE,
                item.cover().ID, item.cover().URL
            )
            .from(DOWNLOADING_ITEM)
            .where(DOWNLOADING_ITEM.STATE.eq(DownloadingState.WAITING))
            .orderBy(DOWNLOADING_ITEM.POSITION.asc())
            .fetch()

        return items.map(::toDownloadingItem)
    }

    fun startItem(id: UUID) {
        query
            .update(DOWNLOADING_ITEM)
            .set(DOWNLOADING_ITEM.STATE, DownloadingState.DOWNLOADING)
            .where(DOWNLOADING_ITEM.ITEM_ID.eq(id))
            .execute()
    }

    fun remove(id: UUID, hasToBeStopped: Boolean) {
        query
            .deleteFrom(DOWNLOADING_ITEM)
            .where(DOWNLOADING_ITEM.ITEM_ID.eq(id))
            .execute()

        if (hasToBeStopped) stopItem(id)
    }

    fun moveItemInQueue(id: UUID, position: Int) {
        val numberOfDownloadingItem = select(coalesce(max(DOWNLOADING_ITEM.POSITION), 0))
            .from(DOWNLOADING_ITEM)
            .where(DOWNLOADING_ITEM.STATE.eq(DownloadingState.DOWNLOADING))
            .asField<Int>()

        val currentPosition = select(DOWNLOADING_ITEM.POSITION)
            .from(DOWNLOADING_ITEM)
            .where(DOWNLOADING_ITEM.ITEM_ID.eq(id)).asField<Int>()

        val isMovingDown = select(field(value(position).gt(DOWNLOADING_ITEM.POSITION - 1 - numberOfDownloadingItem)))
            .from(DOWNLOADING_ITEM)
            .where(DOWNLOADING_ITEM.ITEM_ID.eq(id))

        val isMovingUp = select(field(value(position).lt(DOWNLOADING_ITEM.POSITION - 1 - numberOfDownloadingItem)))
            .from(DOWNLOADING_ITEM)
            .where(DOWNLOADING_ITEM.ITEM_ID.eq(id))

        val updateMoveUp = query
            .update(DOWNLOADING_ITEM)
            .set(DOWNLOADING_ITEM.POSITION, DOWNLOADING_ITEM.POSITION + 1 )
            .where(DOWNLOADING_ITEM.POSITION
                .between(numberOfDownloadingItem + position + 1)
                .and(currentPosition - 1)
            )
            .and(value(true).eq(isMovingUp))
            .returning()

        val updateMoveDown = query
            .update(DOWNLOADING_ITEM)
            .set(DOWNLOADING_ITEM.POSITION, DOWNLOADING_ITEM.POSITION - 1 )
            .where(DOWNLOADING_ITEM.POSITION
                .between(currentPosition + 1)
                .and(numberOfDownloadingItem + position + 1)
            )
            .and(value(true).eq(isMovingDown))
            .returning()

        query
            .with("updateMoveUp").`as`(updateMoveUp)
            .with("updateMoveDown").`as`(updateMoveDown)
            .update(DOWNLOADING_ITEM)
            .set(DOWNLOADING_ITEM.POSITION, numberOfDownloadingItem + position + 1 )
            .where(DOWNLOADING_ITEM.ITEM_ID.eq(id))
            .execute()
    }

    fun stopItem(id: UUID): Int {
        return query
            .update(ITEM)
            .set(ITEM.STATUS, STOPPED)
            .where(ITEM.ID.eq(id))
            .execute()
    }

    fun updateDownloadItem(item: DownloadingItem): Int {
        return query
            .update(ITEM)
            .set(ITEM.STATUS, item.status.toDb())
            .set(ITEM.NUMBER_OF_FAIL, item.numberOfFail)
            .where(ITEM.ID.eq(item.id))
            .execute()
    }

    fun finishDownload(id: UUID, length: Long, mimeType: String, fileName: Path, downloadDate: OffsetDateTime): Int {
        return query
            .update(ITEM)
            .set(ITEM.STATUS, FINISH)
            .set(ITEM.LENGTH, length)
            .set(ITEM.MIME_TYPE, mimeType)
            .set(ITEM.FILE_NAME, fileName)
            .set(ITEM.DOWNLOAD_DATE, downloadDate)
            .where(ITEM.ID.eq(id))
            .execute()
    }

    fun resetToWaitingStateAllDownloadingItems(): Int {
        return query.
        update(DOWNLOADING_ITEM)
            .set(DOWNLOADING_ITEM.STATE, DownloadingState.WAITING)
            .where(DOWNLOADING_ITEM.STATE.eq(DownloadingState.DOWNLOADING))
            .execute()
    }
}

private fun toDownloadingItem(
    it: Record9<UUID, String, ItemStatus, String, Int, UUID, String, UUID, String>,
    base: Item = DOWNLOADING_ITEM.item()
): DownloadingItem {
    return DownloadingItem(
        id = it[base.ID],
        title = it[base.TITLE],
        status = it[base.STATUS].fromDb(),
        url = URI(it[base.URL]),
        numberOfFail = it[base.NUMBER_OF_FAIL],
        progression = 0,
        podcast = Podcast(it[base.podcast().ID], it[base.podcast().TITLE]),
        cover = Cover(it[base.cover().ID], URI(it[base.cover().URL])),
    )
}
