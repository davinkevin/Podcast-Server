package com.github.davinkevin.podcastserver.playlist

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import java.util.*

class PlaylistHandler(
        private val playlistService: PlaylistService
) {

    fun findAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): Mono<ServerResponse> =
            playlistService
                    .findAll()
                    .map { WatchListHAL(it.id, it.name) }
                    .collectList()
                    .map { FindAllWatchListHAL(it) }
                    .flatMap { ok().bodyValue(it) }

}

private class FindAllWatchListHAL(val content: Collection<WatchListHAL>)
private class WatchListHAL(val id: UUID, val name: String)
