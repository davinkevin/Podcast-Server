package com.github.davinkevin.podcastserver.podcast

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.router

/**
 * Created by kevin on 2019-02-15
 */
@Configuration
class PodcastRoutingConfig {

    @Bean
    fun podcastRouter(podcast: PodcastHandler) = router {
        "/api/v1/podcasts".nest {
            GET("/{id}/cover.{ext}", podcast::cover)

            GET("/{id}/stats/byPubDate", podcast::findStatByPubDate)
            GET("/{id}/stats/byDownloadDate", podcast::findStatByDownloadDate)
            GET("/{id}/stats/byCreationDate", podcast::findStatByCreationDate)
        }
    }
}

@Configuration
@Import(PodcastRepositoryV2::class, PodcastRoutingConfig::class, PodcastService::class, PodcastHandler::class)
class PodcastConfig
