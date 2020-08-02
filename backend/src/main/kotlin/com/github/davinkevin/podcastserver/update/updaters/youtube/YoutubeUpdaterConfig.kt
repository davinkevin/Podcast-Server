package com.github.davinkevin.podcastserver.update.updaters.youtube

import com.github.davinkevin.podcastserver.update.updaters.Updater
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import com.github.davinkevin.podcastserver.service.properties.Api
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

/**
 * Created by kevin on 31/08/2019
 */
@Configuration
@Import(ImageServiceConfig::class)
@EnableConfigurationProperties(Api::class)
class YoutubeUpdaterConfig {

    @Bean
    fun youtubeUpdater(
            api: Api,
            wcb: WebClient.Builder
    ): Updater {
        val key = api.youtube

        val builder = wcb
                .clone()
                .clientConnector(ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))

        val youtubeClient = builder.clone()
                .baseUrl("https://www.youtube.com")
                .build()

        if (key.isEmpty()) {
         return YoutubeByXmlUpdater(youtubeClient)
        }

        return YoutubeByApiUpdater(
                key = key,
                youtubeClient = youtubeClient,
                googleApiClient = builder.clone().baseUrl("https://www.googleapis.com").build()
        )
    }
}
