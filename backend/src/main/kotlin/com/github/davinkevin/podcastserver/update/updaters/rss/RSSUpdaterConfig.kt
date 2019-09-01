package com.github.davinkevin.podcastserver.update.updaters.rss

import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

/**
 * Created by kevin on 01/09/2019
 */
@Configuration
@Import(ImageServiceConfig::class)
class RSSUpdaterConfig {

    @Bean
    fun rssUpdater(imageService: ImageService, wcb: WebClient.Builder): RSSUpdater {
        val builder = wcb
                .clone()
                .clientConnector(ReactorClientHttpConnector(HttpClient.create().followRedirect { _, res -> res.status().code() in 300..399 }))

        return RSSUpdater(imageService, builder)
    }
}
