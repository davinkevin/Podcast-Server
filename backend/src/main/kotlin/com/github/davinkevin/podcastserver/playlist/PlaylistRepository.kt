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
            .select(
                PLAYLIST.ID, PLAYLIST.NAME,
                PLAYLIST.cover().ID, PLAYLIST.cover().HEIGHT, PLAYLIST.cover().WIDTH, PLAYLIST.cover().URL,
            )
            .from(PLAYLIST)
            .where(PLAYLIST.ID.eq(id))
            .fetchOne()
            ?: return null

        val items = query
                .select(
                    ITEM.ID, ITEM.TITLE, ITEM.URL,
                    ITEM.FILE_NAME, ITEM.DESCRIPTION, ITEM.MIME_TYPE, ITEM.LENGTH, ITEM.PUB_DATE,

                    PODCAST.ID, PODCAST.TITLE,
                    COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT,
                )
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
            items = items,
            cover = PlaylistWithItems.Cover(
                id = playlist[COVER.ID],
                height = playlist[COVER.HEIGHT],
                width = playlist[COVER.WIDTH],
                url = URI(playlist[COVER.URL]),
            ),
        )
    }

    fun save(request: SaveRequest): PlaylistWithItems {
        val (name, cover) = request
        val playlistId = UUID.randomUUID()
        val coverId = UUID.randomUUID()

        var answerIfInsert: PlaylistWithItems? = null
        var isInserted: Boolean = false
        query.transaction { trx ->
            trx.dsl()
                .insertInto(COVER)
                .set(COVER.ID, coverId)
                .set(COVER.URL, cover.url.toASCIIString())
                .set(COVER.HEIGHT, cover.height)
                .set(COVER.WIDTH, cover.width)
                .execute()

            isInserted = trx.dsl()
                .insertInto(PLAYLIST)
                .set(PLAYLIST.ID, playlistId)
                .set(PLAYLIST.NAME, name)
                .set(PLAYLIST.COVER_ID, coverId)
                .onConflictDoNothing()
                .execute() == 1
        }

        if(isInserted) {
            return PlaylistWithItems(
                id = playlistId,
                name = name,
                items = emptyList(),
                cover = PlaylistWithItems.Cover(
                    id = coverId,
                    height = cover.height,
                    width = cover.width,
                    url = cover.url
                )
            )
        }

        val id = query
                .select(PLAYLIST.ID)
                .from(PLAYLIST)
                .where(PLAYLIST.NAME.eq(name))
                .fetch()
                .first()
                .let { (v) -> v }

        return findById(id)!!
    }
    data class SaveRequest(val name: String, val cover: Cover) {
        data class Cover(val url: URI, val height: Int, val width: Int)
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
