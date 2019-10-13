package com.github.davinkevin.podcastserver.playlist

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import com.github.davinkevin.podcastserver.playlist.PlaylistRepositoryV2 as WatchListRepository

class PlaylistService(
        private val repository: WatchListRepository
) {

    fun findAll(): Flux<Playlist> = repository.findAll()
    fun findById(id: UUID): Mono<PlaylistWithItems> = repository.findById(id)

}
