package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.database.Tables.PODCAST
import com.github.davinkevin.podcastserver.database.tables.Cover.COVER
import com.github.davinkevin.podcastserver.service.storage.DeleteRequest
import org.jooq.DSLContext
import java.net.URI
import java.time.OffsetDateTime
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.extension

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
            .orderBy(COVER.ID.asc())
            .fetch()
            .map { (podcastId, podcastTitle, itemId, itemTitle, coverId, coverUrl) ->
                DeleteRequest.ForCover(
                    id = coverId,
                    extension = Path(coverUrl).extension,
                    item = DeleteRequest.ForCover.Item(itemId, itemTitle),
                    podcast = DeleteRequest.ForCover.Podcast(podcastId, podcastTitle)
                )
            }
    }
}

data class CoverForCreation(val width: Int, val height: Int, val url: URI)