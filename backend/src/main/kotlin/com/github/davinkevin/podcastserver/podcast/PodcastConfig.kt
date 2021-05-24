package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.podcast.type.TypeConfig
import kotlinx.coroutines.FlowPreview
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.coRouter
import org.springframework.web.reactive.function.server.router

/**
 * Created by kevin on 2019-02-15
 */
@OptIn(FlowPreview::class)
@Configuration
@Import(PodcastHandler::class, PodcastXmlHandler::class)
class PodcastRoutingConfig {

    @Bean
    fun podcastRouter(podcast: PodcastHandler, xml: PodcastXmlHandler) = router {
        "/api/v1/podcasts".nest {
            val id = "{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}"
            GET("", podcast::findAll)
            GET("${id}", podcast::findById)
            POST("", podcast::create)
            PUT("${id}", podcast::update)

            "{id}".nest {
                GET("cover.{ext}", podcast::cover)


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

    @Bean
    fun coPodcastRouter(xml: PodcastXmlHandler) = coRouter {
        "/api/v1/podcasts".nest {
            GET("opml", xml::opml)
            "{id}".nest {
                GET("rss", xml::rss)
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
