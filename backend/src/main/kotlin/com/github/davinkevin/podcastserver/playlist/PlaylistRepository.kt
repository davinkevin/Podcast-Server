package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.extension.repository.toUTC
import org.jooq.DSLContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.util.*

class PlaylistRepositoryV2(
        private val query: DSLContext
) {

    fun findAll(): Flux<Playlist> = Flux.defer {
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

    fun findById(id: UUID): Mono<PlaylistWithItems> = Mono.defer {
        val items = Flux.from(
                query
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
        )
                .map { PlaylistWithItems.Item(
                        id = it[ITEM.ID],
                        title = it[ITEM.TITLE],
                        fileName = it[ITEM.FILE_NAME],
                        description = it[ITEM.DESCRIPTION],
                        mimeType = it[ITEM.MIME_TYPE],
                        length = it[ITEM.LENGTH],
                        pubDate = it[ITEM.PUB_DATE].toUTC(),
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
                .select(WATCH_LIST.ID, WATCH_LIST.NAME)
                .from(WATCH_LIST)
                .where(WATCH_LIST.ID.eq(id))
                .toMono()

        Mono.zip(playlist, items)
                .map { (pl, items) -> PlaylistWithItems(
                        id = pl[WATCH_LIST.ID],
                        name = pl[WATCH_LIST.NAME],
                        items = items
                ) }
    }

    fun save(name: String): Mono<PlaylistWithItems> = Mono.defer {
        val id = UUID.randomUUID()

        query
                .insertInto(WATCH_LIST)
                .set(WATCH_LIST.ID, id)
                .set(WATCH_LIST.NAME, name)
                .onConflictDoNothing()
                .toMono()
                .filter { it == 1 }
                .map { PlaylistWithItems(id, name, emptyList()) }
                .switchIfEmpty {
                    query
                            .select(WATCH_LIST.ID)
                            .from(WATCH_LIST)
                            .where(WATCH_LIST.NAME.eq(name))
                            .toMono()
                            .flatMap { findById(it[WATCH_LIST.ID]) }
                }

    }

    fun addToPlaylist(playlistId: UUID, itemId: UUID): Mono<PlaylistWithItems> = Mono.defer {
        query
                .insertInto(WATCH_LIST_ITEMS)
                .set(WATCH_LIST_ITEMS.WATCH_LISTS_ID, playlistId)
                .set(WATCH_LIST_ITEMS.ITEMS_ID, itemId)
                .onConflictDoNothing()
                .toMono()
                .then(findById(playlistId))
    }

    fun removeFromPlaylist(playlistId: UUID, itemId: UUID): Mono<PlaylistWithItems> = Mono.defer {
        query
                .deleteFrom(WATCH_LIST_ITEMS)
                .where(WATCH_LIST_ITEMS.WATCH_LISTS_ID.eq(playlistId))
                .and(WATCH_LIST_ITEMS.ITEMS_ID.eq(itemId))
                .toMono()
                .then(findById(playlistId))
    }

    fun deleteById(id: UUID): Mono<Void> = Mono.defer {
        query
                .deleteFrom(WATCH_LIST_ITEMS)
                .where(WATCH_LIST_ITEMS.WATCH_LISTS_ID.eq(id))
                .toMono()
                .then(query
                        .deleteFrom(WATCH_LIST)
                        .where(WATCH_LIST.ID.eq(id))
                        .toMono()
                )
                .then()
    }

}
