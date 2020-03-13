package com.github.davinkevin.podcastserver.update

import com.github.davinkevin.podcastserver.update.updaters.dailymotion.DailymotionUpdaterConfig
import com.github.davinkevin.podcastserver.update.updaters.francetv.FranceTvUpdaterConfig
import com.github.davinkevin.podcastserver.update.updaters.rss.RSSUpdaterConfig
import com.github.davinkevin.podcastserver.update.updaters.mytf1.MyTf1UpdaterConfig
import com.github.davinkevin.podcastserver.update.updaters.youtube.YoutubeUpdaterConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.router

/**
 * Created by kevin on 2019-08-10
 */
@Configuration
class UpdateRouterConfig {

    @Bean
    fun updateRouter(update: UpdateHandler) = router {
        "/api/v1/podcasts".nest {
            GET("/update", update::updateAll)
            GET("/{podcastId}/update", update::update)
        }
    }
}

@Configuration
@Import(
        UpdateRouterConfig::class,
        UpdateHandler::class,
        UpdateService::class,

        DailymotionUpdaterConfig::class,
        FranceTvUpdaterConfig::class,
        RSSUpdaterConfig::class,
        MyTf1UpdaterConfig::class,
        YoutubeUpdaterConfig::class
)
class UpdateConfig
