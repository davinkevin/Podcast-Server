package com.github.davinkevin.podcastserver.playlist

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.coRouter
import org.springframework.web.reactive.function.server.router

/**
 * Created by kevin on 2019-07-01
 */
@Configuration
class PlaylistRoutingConfig {

    @Bean
    fun playlistRouter(playlist: PlaylistHandler) = coRouter {
        "/api/v1/playlists".nest {
            GET("", playlist::findAll)
            POST("", playlist::save)
            GET("{id}", playlist::findById)
            DELETE("{id}", playlist::deleteById)
            GET("{id}/rss", playlist::rss)
            "{id}/items/{itemId}".nest {
                POST("", playlist::addToPlaylist)
                DELETE("", playlist::removeFromPlaylist)
            }
        }
    }

}

@Configuration
@Import(
        PlaylistRoutingConfig::class,
        PlaylistHandler::class,
        PlaylistService::class,
        PlaylistRepository::class
)
class PlaylistConfig
