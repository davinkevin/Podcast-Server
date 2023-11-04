package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.database.Tables.*
import org.jooq.DSLContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.util.*

class PlaylistRepository(
        private val query: DSLContext
) {

    fun findAll(): Flux<Playlist> = Flux.defer {
        Flux.from(query
                .select(
                    PLAYLIST.ID, PLAYLIST.NAME,
                    COVER.ID, COVER.WIDTH, COVER.HEIGHT, COVER.URL,
                )
                .from(PLAYLIST)
                    .innerJoin(COVER)
                    .on(COVER.ID.eq(PLAYLIST.COVER_ID))
                .orderBy(PLAYLIST.NAME)
        )
                .map { Playlist(
                        id = it[PLAYLIST.ID],
                        name = it[PLAYLIST.NAME],
                        cover = Playlist.Cover(
                            id = it[COVER.ID],
                            height = it[COVER.HEIGHT],
                            width = it[COVER.WIDTH],
                            url = URI.create(it[COVER.URL]),
                        )
                    )
                }
    }

    fun findById(id: UUID): Mono<Playlist> = Mono.defer {
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
            .select(
                PLAYLIST.ID, PLAYLIST.NAME,
                COVER.ID, COVER.WIDTH, COVER.HEIGHT, COVER.URL,
            )
            .from(PLAYLIST)
            .innerJoin(COVER)
                .on(COVER.ID.eq(PLAYLIST.COVER_ID))
            .where(PLAYLIST.ID.eq(id))
            .toMono()

//        Mono.zip(playlist, items)
//                .map { (pl, items) -> PlaylistWithItems(
//                        id = pl[PLAYLIST.ID],
//                        name = pl[PLAYLIST.NAME],
//                        items = items,
//                ) }
        TODO()
    }

    fun create(playlist: PlaylistForDatabaseCreation): Mono<Playlist> = Mono.defer {
        TODO()
//        val id = UUID.randomUUID()
//        val coverId = UUID.randomUUID()
//
//        query
//            .with(name("COVER_CREATION").`as`(
//                insertInto(COVER)
//                    .set(COVER.ID, coverId)
//                    .set(COVER.URL, playlist.cover.url.toASCIIString())
//                    .set(COVER.WIDTH, playlist.cover.width)
//                    .set(COVER.HEIGHT, playlist.cover.height)
//                    .returning(COVER.ID)
//            ))
//            .insertInto(PLAYLIST)
//            .set(PLAYLIST.ID, id)
//            .set(PLAYLIST.NAME, playlist.name)
//            .set(PLAYLIST.COVER_ID, coverId)
//            .returning(PLAYLIST.ID)
//            .toMono()
//            .map {PlaylistWithItems(
//                    id = id,
//                    name = playlist.name,
//                    items = emptyList()
//                )
//            }

    }

    fun addToPlaylist(playlistId: UUID, itemId: UUID): Mono<PlaylistWithItems> = Mono.defer {
        TODO()
//        query
//                .insertInto(PLAYLIST_ITEMS)
//                .set(PLAYLIST_ITEMS.PLAYLISTS_ID, playlistId)
//                .set(PLAYLIST_ITEMS.ITEMS_ID, itemId)
//                .onConflictDoNothing()
//                .toMono()
//                .then(findById(playlistId))
    }

    fun removeFromPlaylist(playlistId: UUID, itemId: UUID): Mono<PlaylistWithItems> = Mono.defer {
        TODO()
//        query
//                .deleteFrom(PLAYLIST_ITEMS)
//                .where(PLAYLIST_ITEMS.PLAYLISTS_ID.eq(playlistId))
//                .and(PLAYLIST_ITEMS.ITEMS_ID.eq(itemId))
//                .toMono()
//                .then(findById(playlistId))
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
data class PlaylistForDatabaseCreation(val name: String, val cover: CoverForCreation)
