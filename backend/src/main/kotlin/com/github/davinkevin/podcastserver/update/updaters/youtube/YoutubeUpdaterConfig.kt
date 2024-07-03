package com.github.davinkevin.podcastserver.update.updaters.youtube

import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import com.github.davinkevin.podcastserver.update.updaters.Updater
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

/**
 * Created by kevin on 31/08/2019
 */
@Configuration
@Import(ImageServiceConfig::class)
@EnableConfigurationProperties(YoutubeApi::class)
class YoutubeUpdaterConfig {

    @Bean
    fun youtubeUpdater(
        api: YoutubeApi,
        rcb: RestClient.Builder,
    ): Updater {
        val key = api.youtube

        val builder = rcb
            .clone()
            .defaultHeader("User-Agent", "curl/7.64.1")

        val youtubeClient = builder
            .clone()
            .baseUrl("https://www.youtube.com")
            .build()

        if (key.isEmpty()) {
            return YoutubeByXmlUpdater(youtubeClient)
        }

        return YoutubeByApiUpdater(
            key = key,
            youtube = youtubeClient,
            googleApi = builder.clone().baseUrl("https://www.googleapis.com").build()
        )
    }
}

@ConfigurationProperties("podcastserver.api")
data class YoutubeApi(val youtube: String = "")
