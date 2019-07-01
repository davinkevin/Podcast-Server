package com.github.davinkevin.podcastserver.watchlist

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import com.github.davinkevin.podcastserver.watchlist.WatchListRepositoryV2 as WatchListRepository

class WatchListService(
        private val repository: WatchListRepository
) {

    fun findAll(): Flux<WatchList> = repository.findAll()

}
