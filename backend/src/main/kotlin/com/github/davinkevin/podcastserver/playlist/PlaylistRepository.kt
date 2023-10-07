package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.database.Tables.*
import org.jooq.DSLContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.util.*

class PlaylistRepository(
        private val query: DSLContext
) {

    fun findAll(): Flux<Playlist> = Flux.defer {
        Flux.from(query
                .select(PLAYLIST.ID, PLAYLIST.NAME)
                .from(PLAYLIST)
                .orderBy(PLAYLIST.NAME)
        )
                .map { Playlist(
                        id = it[PLAYLIST.ID],
                        name = it[PLAYLIST.NAME]
                ) }
    }

    fun findById(id: UUID): Mono<PlaylistWithItems> = Mono.defer {
        val items = Flux.from(
                query
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
        )
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
                .collectList()

        val playlist = query
                .select(PLAYLIST.ID, PLAYLIST.NAME)
                .from(PLAYLIST)
                .where(PLAYLIST.ID.eq(id))
                .toMono()

        Mono.zip(playlist, items)
                .map { (pl, items) -> PlaylistWithItems(
                        id = pl[PLAYLIST.ID],
                        name = pl[PLAYLIST.NAME],
                        items = items
                ) }
    }

    fun save(name: String): Mono<PlaylistWithItems> = Mono.defer {
        val id = UUID.randomUUID()

        query
                .insertInto(PLAYLIST)
                .set(PLAYLIST.ID, id)
                .set(PLAYLIST.NAME, name)
                .onConflictDoNothing()
                .toMono()
                .filter { it == 1 }
                .map { PlaylistWithItems(id, name, emptyList()) }
                .switchIfEmpty {
                    query
                            .select(PLAYLIST.ID)
                            .from(PLAYLIST)
                            .where(PLAYLIST.NAME.eq(name))
                            .toMono()
                            .flatMap { findById(it[PLAYLIST.ID]) }
                }

    }

    fun addToPlaylist(playlistId: UUID, itemId: UUID): Mono<PlaylistWithItems> = Mono.defer {
        query
                .insertInto(PLAYLIST_ITEMS)
                .set(PLAYLIST_ITEMS.PLAYLISTS_ID, playlistId)
                .set(PLAYLIST_ITEMS.ITEMS_ID, itemId)
                .onConflictDoNothing()
                .toMono()
                .then(findById(playlistId))
    }

    fun removeFromPlaylist(playlistId: UUID, itemId: UUID): Mono<PlaylistWithItems> = Mono.defer {
        query
                .deleteFrom(PLAYLIST_ITEMS)
                .where(PLAYLIST_ITEMS.PLAYLISTS_ID.eq(playlistId))
                .and(PLAYLIST_ITEMS.ITEMS_ID.eq(itemId))
                .toMono()
                .then(findById(playlistId))
    }

    fun deleteById(id: UUID): Mono<Void> = Mono.defer {
        query
                .deleteFrom(PLAYLIST_ITEMS)
                .where(PLAYLIST_ITEMS.PLAYLISTS_ID.eq(id))
                .toMono()
                .then(query
                        .deleteFrom(PLAYLIST)
                        .where(PLAYLIST.ID.eq(id))
                        .toMono()
                )
                .then()
    }

}
