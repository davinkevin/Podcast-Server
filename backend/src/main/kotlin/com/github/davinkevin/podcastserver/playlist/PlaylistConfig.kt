package com.github.davinkevin.podcastserver.playlist

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.router

/**
 * Created by kevin on 2019-07-01
 */
@Configuration
class PlaylistRoutingConfig {

    @Bean
    fun watchlistRouter(playlist: PlaylistHandler) = router {
        "/api/v1/playlists".nest {
            GET("/", playlist::findAll)
        }
    }

}

@Configuration
@Import(PlaylistRoutingConfig::class, PlaylistHandler::class, PlaylistService::class, PlaylistRepositoryV2::class)
class PlaylistConfig
