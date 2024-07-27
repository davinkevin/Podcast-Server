package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.cover.DeleteCoverRequest.Item
import com.github.davinkevin.podcastserver.cover.DeleteCoverRequest.Podcast
import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.database.Tables.PODCAST
import com.github.davinkevin.podcastserver.database.tables.Cover.COVER
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

    fun findCoverOlderThan(date: OffsetDateTime): List<DeleteCoverRequest> {
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
                DeleteCoverRequest(
                    id = coverId,
                    extension = Path(coverUrl).extension,
                    item = Item(itemId, itemTitle),
                    podcast = Podcast(podcastId, podcastTitle)
                )
            }
    }
}

data class CoverForCreation(val width: Int, val height: Int, val url: URI)
data class DeleteCoverRequest(val id: UUID, val extension: String, val item: Item, val podcast: Podcast) {
    data class Item(val id: UUID, val title: String)
    data class Podcast(val id: UUID, val title: String)
}
