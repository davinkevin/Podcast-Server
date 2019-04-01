package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.podcast.type.TypeConfig
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

            GET("/{id}", podcast::findById)
            POST("/", podcast::create)

            "/{id}".nest {
                GET("/cover.{ext}", podcast::cover)

                "/stats".nest {
                    GET("/byPubDate", podcast::findStatByPodcastIdAndPubDate)
                    GET("/byDownloadDate", podcast::findStatByPodcastIdAndDownloadDate)
                    GET("/byCreationDate", podcast::findStatByPodcastIdAndCreationDate)
                }

            }

            "/stats".nest {
                GET("/byCreationDate", podcast::findStatByTypeAndCreationDate)
                GET("/byPubDate", podcast::findStatByTypeAndPubDate)
                GET("/byDownloadDate", podcast::findStatByTypeAndDownloadDate)
            }

        }
    }
}

@Configuration
@Import(PodcastRepositoryV2::class, TypeConfig::class, PodcastRoutingConfig::class, PodcastService::class, PodcastHandler::class)
class PodcastConfig
