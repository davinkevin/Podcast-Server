package com.github.davinkevin.podcastserver.watchlist

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.router

/**
 * Created by kevin on 2019-07-01
 */
@Configuration
class WatchListRoutingConfig {

    @Bean
    fun watchlistRouter(watchList: WatchListHandler) = router {
        "/api/v1/watchlists".nest {
            GET("/", watchList::findAll)
        }
    }

}

@Configuration
@Import(WatchListRoutingConfig::class, WatchListHandler::class, WatchListService::class, WatchListRepositoryV2::class)
class WatchListConfig
