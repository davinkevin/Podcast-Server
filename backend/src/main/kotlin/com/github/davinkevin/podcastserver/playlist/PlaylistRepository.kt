package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.database.Tables.*
import org.jooq.DSLContext
import java.net.URI
import java.util.*

class PlaylistRepository(
    private val query: DSLContext
) {

    fun findAll(): List<Playlist> =
        query
            .select(WATCH_LIST.ID, WATCH_LIST.NAME)
            .from(WATCH_LIST)
            .orderBy(WATCH_LIST.NAME)
            .fetch()
            .map { Playlist(
                id = it[WATCH_LIST.ID],
                name = it[WATCH_LIST.NAME]
            ) }

    fun findById(id: UUID): PlaylistWithItems? {
        val playlist = query
            .select(WATCH_LIST.ID, WATCH_LIST.NAME)
            .from(WATCH_LIST)
            .where(WATCH_LIST.ID.eq(id))
            .fetchOne()
            ?: return null

        val items = query
                .select(ITEM.ID, ITEM.TITLE, ITEM.URL,

                    ITEM.FILE_NAME, ITEM.DESCRIPTION, ITEM.MIME_TYPE, ITEM.LENGTH, ITEM.PUB_DATE,

                    PODCAST.ID, PODCAST.TITLE,
                    COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                .from(
                    WATCH_LIST_ITEMS
                        .innerJoin(ITEM).on(WATCH_LIST_ITEMS.ITEMS_ID.eq(ITEM.ID))
                        .innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID))
                        .innerJoin(COVER).on(ITEM.COVER_ID.eq(COVER.ID))
                )
                .where(WATCH_LIST_ITEMS.WATCH_LISTS_ID.eq(id))
            .fetch()
            .map { PlaylistWithItems.Item(
                id = it[ITEM.ID],
                title = it[ITEM.TITLE],
                fileName = it[ITEM.FILE_NAME],
                description = it[ITEM.DESCRIPTION],
                mimeType = it[ITEM.MIME_TYPE],
                length = it[ITEM.LENGTH],
                pubDate = it[ITEM.PUB_DATE],
                podcast = PlaylistWithItems.Item.Podcast(
                    id = it[PODCAST.ID],
                    title = it[PODCAST.TITLE]
                ),
                cover = PlaylistWithItems.Item.Cover(
                    id = it[COVER.ID],
                    width = it[COVER.WIDTH],
                    height = it[COVER.HEIGHT],
                    url = URI(it[COVER.URL])
                )
            ) }

        return PlaylistWithItems(
            id = playlist[WATCH_LIST.ID],
            name = playlist[WATCH_LIST.NAME],
            items = items
        )
    }

    fun save(name: String): PlaylistWithItems {
        val id = UUID.randomUUID()

        val numberOfRowInserted = query
            .insertInto(WATCH_LIST)
            .set(WATCH_LIST.ID, id)
            .set(WATCH_LIST.NAME, name)
            .onConflictDoNothing()
            .execute()

        if (numberOfRowInserted == 1) {
            return PlaylistWithItems(id, name, emptyList())
        }

        val playlist = query
            .select(WATCH_LIST.ID)
            .from(WATCH_LIST)
            .where(WATCH_LIST.NAME.eq(name))
            .fetch()
            .first()

        return findById(playlist[WATCH_LIST.ID])!!
    }

    fun addToPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems {
        query
            .insertInto(WATCH_LIST_ITEMS)
            .set(WATCH_LIST_ITEMS.WATCH_LISTS_ID, playlistId)
            .set(WATCH_LIST_ITEMS.ITEMS_ID, itemId)
            .onConflictDoNothing()
            .execute()

        return findById(playlistId)!!
    }

    fun removeFromPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems {
        query
            .deleteFrom(WATCH_LIST_ITEMS)
            .where(WATCH_LIST_ITEMS.WATCH_LISTS_ID.eq(playlistId))
            .and(WATCH_LIST_ITEMS.ITEMS_ID.eq(itemId))
            .execute()

        return findById(playlistId)!!
    }

    fun deleteById(id: UUID) {
        query
            .deleteFrom(WATCH_LIST_ITEMS)
            .where(WATCH_LIST_ITEMS.WATCH_LISTS_ID.eq(id))
            .execute()

        query
            .deleteFrom(WATCH_LIST)
            .where(WATCH_LIST.ID.eq(id))
            .execute()
    }

}
