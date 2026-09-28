package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.database.Tables.PLAYLIST_ITEMS
import com.github.davinkevin.podcastserver.database.Tables.PODCAST
import com.github.davinkevin.podcastserver.database.enums.ItemStatus.FINISH
import com.github.davinkevin.podcastserver.database.tables.Cover.COVER
import com.github.davinkevin.podcastserver.extension.java.net.coverExtension
import com.github.davinkevin.podcastserver.service.storage.DeleteRequest
import org.jooq.DSLContext
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

class CoverRepository(private val query: DSLContext) {

    fun save(cover: CoverForCreation): Cover {
        val id = UUID.randomUUID()

        query.insertInto(COVER)
            .set(COVER.ID, id)
            .set(COVER.WIDTH, cover.width)
            .set(COVER.HEIGHT, cover.height)
            .set(COVER.URL, cover.url.toASCIIString())
            .execute()

        return Cover(id, cover.url, cover.height, cover.width)
    }

    fun findCoverOlderThan(date: OffsetDateTime): List<DeleteRequest.ForCover> {
        return query
            .select(
                PODCAST.ID, PODCAST.TITLE,
                ITEM.ID, ITEM.TITLE,
                COVER.ID, COVER.URL
            )
            .from(
                COVER
                    .innerJoin(ITEM).on(COVER.ID.eq(ITEM.COVER_ID))
                    .innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID))
            )
            .where(ITEM.CREATION_DATE.lessOrEqual(date))
            .and(ITEM.STATUS.ne(FINISH))
            .and(PODCAST.HAS_TO_BE_DELETED.isTrue)
            .and(ITEM.ID.notIn(query.select(PLAYLIST_ITEMS.ITEMS_ID).from(PLAYLIST_ITEMS)))
            .orderBy(COVER.ID.asc())
            .fetch()
            .map { (podcastId, podcastTitle, itemId, itemTitle, coverId, coverUrl) ->
                DeleteRequest.ForCover(
                    id = coverId,
                    extension = URI(coverUrl).coverExtension(),
                    item = DeleteRequest.ForCover.Item(itemId, itemTitle),
                    podcast = DeleteRequest.ForCover.Podcast(podcastId, podcastTitle)
                )
            }
    }
}

data class CoverForCreation(val width: Int, val height: Int, val url: URI)