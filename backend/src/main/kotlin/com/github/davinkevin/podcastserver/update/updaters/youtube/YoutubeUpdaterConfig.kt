package com.github.davinkevin.podcastserver.update.updaters.youtube

import com.github.davinkevin.podcastserver.manager.worker.Updater
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.service.properties.Api
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

/**
 * Created by kevin on 31/08/2019
 */
@Configuration
class YoutubeUpdaterConfig {

    @Bean
    fun youtubeUpdater(
            api: Api,
            jdomService: JdomService,
            htmlService: HtmlService,
            wcb: WebClient.Builder
    ): Updater {
        val youtubeKey = api.youtube

        if (youtubeKey.isNullOrEmpty()) {
         return YoutubeByXmlUpdater(jdomService, htmlService)
        }

        val builder = wcb
                .clone()
                .clientConnector(ReactorClientHttpConnector(HttpClient.create().followRedirect { _, res -> res.status().code() in 300..399 }))

        return YoutubeByApiUpdater(
                key = youtubeKey,
                youtubeClient = builder.clone().baseUrl("https://www.youtube.com").build(),
                googleApiClient = builder.clone().baseUrl("https://www.googleapis.com").build()
        )
    }
}
