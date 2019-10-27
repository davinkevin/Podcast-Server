package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.extension.repository.executeAsyncAsMono
import com.github.davinkevin.podcastserver.extension.repository.fetchAsFlux
import com.github.davinkevin.podcastserver.extension.repository.fetchOneAsMono
import org.jooq.DSLContext
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.util.*

class PlaylistRepositoryV2(
        private val query: DSLContext
) {

    fun findAll() = query
            .select(WATCH_LIST.ID, WATCH_LIST.NAME)
            .from(WATCH_LIST)
            .orderBy(WATCH_LIST.NAME)
            .fetchAsFlux()
            .map { Playlist(
                    id = it[WATCH_LIST.ID],
                    name = it[WATCH_LIST.NAME]
            ) }

    fun findById(id: UUID): Mono<PlaylistWithItems> = Mono.defer {
        val items = query
                .select(ITEM.ID, ITEM.TITLE, ITEM.URL,

                        ITEM.FILE_NAME, ITEM.DESCRIPTION, ITEM.MIME_TYPE,

                        PODCAST.ID, PODCAST.TITLE,
                        COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                .from(
                        WATCH_LIST_ITEMS
                                .innerJoin(ITEM).on(WATCH_LIST_ITEMS.ITEMS_ID.eq(ITEM.ID))
                                .innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID))
                                .innerJoin(COVER).on(ITEM.COVER_ID.eq(COVER.ID))
                )
                .where(WATCH_LIST_ITEMS.WATCH_LISTS_ID.eq(id))
                .fetchAsFlux()
                .map { PlaylistWithItems.Item(
                        id = it[ITEM.ID],
                        title = it[ITEM.TITLE],
                        fileName = it[ITEM.FILE_NAME],
                        description = it[ITEM.DESCRIPTION],
                        mimeType = it[ITEM.MIME_TYPE],
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
                .fetchOneAsMono()

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
                .executeAsyncAsMono()
                .filter { it == 1 }
                .map { PlaylistWithItems(id, name, emptyList()) }
                .switchIfEmpty {
                    query
                            .select(WATCH_LIST.ID)
                            .from(WATCH_LIST)
                            .where(WATCH_LIST.NAME.eq(name))
                            .fetchOneAsMono()
                            .flatMap { findById(it[WATCH_LIST.ID]) }
                }

    }

    fun addToPlaylist(playlistId: UUID, itemId: UUID): Mono<PlaylistWithItems> = Mono.defer {
        query
                .insertInto(WATCH_LIST_ITEMS)
                .set(WATCH_LIST_ITEMS.WATCH_LISTS_ID, playlistId)
                .set(WATCH_LIST_ITEMS.ITEMS_ID, itemId)
                .onConflictDoNothing()
                .executeAsyncAsMono()
                .then(findById(playlistId))
    }

    fun removeFromPlaylist(playlistId: UUID, itemId: UUID): Mono<PlaylistWithItems> = Mono.defer {
        query
                .deleteFrom(WATCH_LIST_ITEMS)
                .where(WATCH_LIST_ITEMS.WATCH_LISTS_ID.eq(playlistId))
                .and(WATCH_LIST_ITEMS.ITEMS_ID.eq(itemId))
                .executeAsyncAsMono()
                .then(findById(playlistId))
    }

    fun deleteById(id: UUID): Mono<Void> = Mono.defer {
        query
                .deleteFrom(WATCH_LIST_ITEMS)
                .where(WATCH_LIST_ITEMS.WATCH_LISTS_ID.eq(id))
                .executeAsyncAsMono()
                .then(query
                        .deleteFrom(WATCH_LIST)
                        .where(WATCH_LIST.ID.eq(id))
                        .executeAsyncAsMono()
                )
                .then()
    }

}
