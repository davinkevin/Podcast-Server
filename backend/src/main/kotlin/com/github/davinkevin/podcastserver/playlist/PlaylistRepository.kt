package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.database.Tables.*
import org.jooq.DSLContext
import org.jooq.impl.DSL.select
import java.net.URI
import java.util.*

class PlaylistRepository(
    private val query: DSLContext
) {

    fun findAll(): List<Playlist> =
        query
            .select(PLAYLIST.ID, PLAYLIST.NAME)
            .from(PLAYLIST)
            .orderBy(PLAYLIST.NAME)
            .fetch()
            .map { Playlist(
                id = it[PLAYLIST.ID],
                name = it[PLAYLIST.NAME]
            ) }

    fun findById(id: UUID): PlaylistWithItems? {
        val playlist = query
            .select(PLAYLIST.ID, PLAYLIST.NAME)
            .from(PLAYLIST)
            .where(PLAYLIST.ID.eq(id))
            .fetchOne()
            ?: return null

        val items = query
                .select(ITEM.ID, ITEM.TITLE, ITEM.URL,

                    ITEM.FILE_NAME, ITEM.DESCRIPTION, ITEM.MIME_TYPE, ITEM.LENGTH, ITEM.PUB_DATE,

                    PODCAST.ID, PODCAST.TITLE,
                    COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                .from(
                    PLAYLIST_ITEMS
                        .innerJoin(ITEM).on(PLAYLIST_ITEMS.ITEMS_ID.eq(ITEM.ID))
                        .innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID))
                        .innerJoin(COVER).on(ITEM.COVER_ID.eq(COVER.ID))
                )
                .where(PLAYLIST_ITEMS.PLAYLISTS_ID.eq(id))
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
            id = playlist[PLAYLIST.ID],
            name = playlist[PLAYLIST.NAME],
            items = items
        )
    }

    fun save(name: String): PlaylistWithItems {
        val id = UUID.randomUUID()

        val numberOfRowInserted = query
            .insertInto(PLAYLIST)
            .set(PLAYLIST.ID, id)
            .set(PLAYLIST.NAME, name)
            .onConflictDoNothing()
            .execute()

        if (numberOfRowInserted == 1) {
            return PlaylistWithItems(id = id, name = name, items = emptyList())
        }

        val playlist = query
            .select(PLAYLIST.ID)
            .from(PLAYLIST)
            .where(PLAYLIST.NAME.eq(name))
            .fetch()
            .first()

        return findById(playlist[PLAYLIST.ID])!!
    }

    fun addToPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems {
        query
            .insertInto(PLAYLIST_ITEMS)
            .set(PLAYLIST_ITEMS.PLAYLISTS_ID, playlistId)
            .set(PLAYLIST_ITEMS.ITEMS_ID, itemId)
            .onConflictDoNothing()
            .execute()

        return findById(playlistId)!!
    }

    fun removeFromPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems {
        query
            .deleteFrom(PLAYLIST_ITEMS)
            .where(PLAYLIST_ITEMS.PLAYLISTS_ID.eq(playlistId))
            .and(PLAYLIST_ITEMS.ITEMS_ID.eq(itemId))
            .execute()

        return findById(playlistId)!!
    }

    fun deleteById(id: UUID) {
        query
            .deleteFrom(PLAYLIST_ITEMS)
            .where(PLAYLIST_ITEMS.PLAYLISTS_ID.eq(id))
            .execute()

        query
            .deleteFrom(PLAYLIST)
            .where(PLAYLIST.ID.eq(id))
            .execute()
    }

}
