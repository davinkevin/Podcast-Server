package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.podcast.type.TypeConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.function.router

/**
 * Created by kevin on 2019-02-15
 */
@Configuration
@Import(PodcastHandler::class, PodcastXmlHandler::class)
class  PodcastRoutingConfig {

    @Bean
    fun podcastRouter(podcast: PodcastHandler, xml: PodcastXmlHandler) = router {
        "/api/v1/podcasts".nest {
            val id = "{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}"
            GET("", podcast::findAll)
            GET("opml", xml::opml)
            GET("$id", podcast::findById)
            POST("", podcast::create)
            PUT("$id", podcast::update)

            "{id}".nest {
                GET("cover.{ext}", podcast::cover)
                GET("rss", xml::rss)
                DELETE("", podcast::delete)
                "stats".nest {
                    GET("byPubDate", podcast::findStatByPodcastIdAndPubDate)
                    GET("byDownloadDate", podcast::findStatByPodcastIdAndDownloadDate)
                    GET("byCreationDate", podcast::findStatByPodcastIdAndCreationDate)
                }
            }

            "stats".nest {
                GET("byCreationDate", podcast::findStatByTypeAndCreationDate)
                GET("byPubDate", podcast::findStatByTypeAndPubDate)
                GET("byDownloadDate", podcast::findStatByTypeAndDownloadDate)
            }
        }
    }

}

@Configuration
@Import(
        TypeConfig::class,

        PodcastRoutingConfig::class,
        PodcastRepository::class,
        PodcastService::class
)
class PodcastConfig
