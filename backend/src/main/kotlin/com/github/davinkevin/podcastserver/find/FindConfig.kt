package com.github.davinkevin.podcastserver.find

import com.github.davinkevin.podcastserver.find.finders.dailymotion.DailymotionFinderConfig
import com.github.davinkevin.podcastserver.find.finders.francetv.FranceTvFinderConfig
import com.github.davinkevin.podcastserver.find.finders.itunes.ItunesFinderConfig
import com.github.davinkevin.podcastserver.find.finders.mytf1.MyTf1FinderConfig
import com.github.davinkevin.podcastserver.find.finders.rss.RSSFinderConfig
import com.github.davinkevin.podcastserver.find.finders.youtube.YoutubeFinderConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.coRouter

/**
 * Created by kevin on 2019-08-11
 */

@Configuration
@Import(FindHandler::class)
class FindRoutingConfig {

    @Bean
    fun findRouting(find: FindHandler) = coRouter {
        POST("/api/v1/podcasts/find", find::find)
    }

}

@Configuration
@Import(
        FindRoutingConfig::class,
        FindService::class,

        DailymotionFinderConfig::class,
        FranceTvFinderConfig::class,
        ItunesFinderConfig::class,
        MyTf1FinderConfig::class,
        RSSFinderConfig::class,
        YoutubeFinderConfig::class,
)
class FindConfig

