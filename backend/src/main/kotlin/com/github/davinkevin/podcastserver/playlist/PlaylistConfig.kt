package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
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
    fun watchlistRouter(playlist: PlaylistHandler, xml: PlaylistXmlHandler) = router {
        "/api/v1/playlists".nest {
            GET("", playlist::findAll)
            POST("", playlist::create)
            GET("{id}", playlist::findById)
            DELETE("{id}", playlist::deleteById)
            GET("{id}/rss", xml::rss)
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
        PlaylistXmlHandler::class,
        PlaylistService::class,
        PlaylistRepository::class,
        ImageServiceConfig::class,
)
class PlaylistConfig
