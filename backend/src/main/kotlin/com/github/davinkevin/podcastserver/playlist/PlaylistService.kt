package com.github.davinkevin.podcastserver.playlist

import reactor.core.publisher.Flux
import com.github.davinkevin.podcastserver.playlist.PlaylistRepositoryV2 as WatchListRepository

class PlaylistService(
        private val repository: WatchListRepository
) {

    fun findAll(): Flux<Playlist> = repository.findAll()

}
