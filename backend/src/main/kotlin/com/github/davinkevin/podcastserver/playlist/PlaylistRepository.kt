package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.database.Tables.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.jooq.DSLContext
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.util.*

class PlaylistRepository(
    private val query: DSLContext
) {

    fun findAll(): Flow<Playlist> = Flux.defer {
        Flux.from(query
            .select(WATCH_LIST.ID, WATCH_LIST.NAME)
            .from(WATCH_LIST)
            .orderBy(WATCH_LIST.NAME)
        )
            .map { Playlist(
                id = it[WATCH_LIST.ID],
                name = it[WATCH_LIST.NAME]
            ) }
    }
        .asFlow()

    suspend fun findById(id: UUID): PlaylistWithItems? {
        val playlist = query
            .select(WATCH_LIST.ID, WATCH_LIST.NAME)
            .from(WATCH_LIST)
            .where(WATCH_LIST.ID.eq(id))
            .toMono()
            .awaitFirstOrNull()
            ?: return null

        val items = Flux.from(
            query
                .select(
                    ITEM.ID, ITEM.TITLE, ITEM.URL,

                    ITEM.FILE_NAME, ITEM.DESCRIPTION, ITEM.MIME_TYPE, ITEM.LENGTH, ITEM.PUB_DATE,

                    PODCAST.ID, PODCAST.TITLE,
                    COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT
                )
                .from(
                    WATCH_LIST_ITEMS
                        .innerJoin(ITEM).on(WATCH_LIST_ITEMS.ITEMS_ID.eq(ITEM.ID))
                        .innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID))
                        .innerJoin(COVER).on(ITEM.COVER_ID.eq(COVER.ID))
                )
                .where(WATCH_LIST_ITEMS.WATCH_LISTS_ID.eq(id))
        )
            .asFlow()
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
            .toList()

        return PlaylistWithItems(
            id = playlist[WATCH_LIST.ID],
            name = playlist[WATCH_LIST.NAME],
            items = items
        )
    }

    suspend fun save(name: String): PlaylistWithItems {
        val id = UUID.randomUUID()

        val numberOfLineModified = query
            .insertInto(WATCH_LIST)
            .set(WATCH_LIST.ID, id)
            .set(WATCH_LIST.NAME, name)
            .onConflictDoNothing()
            .toMono()
            .awaitFirst()

        if (numberOfLineModified == 1) {
            return PlaylistWithItems(id, name, emptyList())
        }

        val playlistId = query
            .select(WATCH_LIST.ID)
            .from(WATCH_LIST)
            .where(WATCH_LIST.NAME.eq(name))
            .toMono()
            .awaitFirst()
            .let { it[WATCH_LIST.ID] }

        return findById(playlistId)!!
    }

    suspend fun addToPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems {
        query
            .insertInto(WATCH_LIST_ITEMS)
            .set(WATCH_LIST_ITEMS.WATCH_LISTS_ID, playlistId)
            .set(WATCH_LIST_ITEMS.ITEMS_ID, itemId)
            .onConflictDoNothing()
            .toMono()
            .awaitFirstOrNull()

        return findById(playlistId)!!
    }

    suspend fun removeFromPlaylist(playlistId: UUID, itemId: UUID): PlaylistWithItems {
        query
            .deleteFrom(WATCH_LIST_ITEMS)
            .where(WATCH_LIST_ITEMS.WATCH_LISTS_ID.eq(playlistId))
            .and(WATCH_LIST_ITEMS.ITEMS_ID.eq(itemId))
            .toMono()
            .awaitFirst()

        return findById(playlistId)!!
    }

    suspend fun deleteById(id: UUID) {
        query
            .deleteFrom(WATCH_LIST_ITEMS)
            .where(WATCH_LIST_ITEMS.WATCH_LISTS_ID.eq(id))
            .toMono()
            .awaitFirstOrNull()

        query
            .deleteFrom(WATCH_LIST)
            .where(WATCH_LIST.ID.eq(id))
            .toMono()
            .awaitFirstOrNull()
    }

}
