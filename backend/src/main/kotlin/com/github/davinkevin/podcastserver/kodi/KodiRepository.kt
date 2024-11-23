package com.github.davinkevin.podcastserver.kodi

import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.database.Tables.PODCAST
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.toDb
import org.jooq.DSLContext
import java.util.*

class KodiRepository(
    private val query: DSLContext
) {

    fun podcasts(): List<Podcast> {
        return query
            .select(PODCAST.ID, PODCAST.TITLE)
            .from(PODCAST)
            .orderBy(PODCAST.TITLE)
            .fetch()
            .map { (id, title) -> Podcast(id, title) }
    }

    fun items(podcastId: UUID): List<Item> {
        return query
            .select(ITEM.ID, ITEM.TITLE, ITEM.PUB_DATE, ITEM.LENGTH, ITEM.FILE_NAME, ITEM.MIME_TYPE)
            .from(ITEM)
            .where(ITEM.PODCAST_ID.eq(podcastId))
            .and(ITEM.STATUS.eq(Status.FINISH.toDb()))
            .orderBy(ITEM.PUB_DATE.desc())
            .fetch()
            .map { (id, title, pubDate, length, fileName, mimeType) -> Item(id, title, pubDate, length, fileName, mimeType) }
    }

}