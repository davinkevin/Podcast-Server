package com.github.davinkevin.podcastserver.watchlist

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import java.util.*

class WatchListHandler(
        private val watchListService: WatchListService
) {

    fun findAll(@Suppress("UNUSED_PARAMETER") r: ServerRequest): Mono<ServerResponse> =
            watchListService
                    .findAll()
                    .map { WatchListHAL(it.id, it.name) }
                    .collectList()
                    .map { FindAllWatchListHAL(it) }
                    .flatMap { ok().syncBody(it) }

}

private class FindAllWatchListHAL(val content: Collection<WatchListHAL>)
private class WatchListHAL(val id: UUID, val name: String)
