package com.github.davinkevin.podcastserver.find.finders.rss

import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import com.github.davinkevin.podcastserver.service.image.ImageService

@Configuration
@Import(ImageServiceConfig::class)
class RSSFinderConfig {

    @Bean
    fun rssFinder(imageService: ImageService, wcb: WebClient.Builder): RSSFinder {
        val builder = wcb.clone()
                .clientConnector(ReactorClientHttpConnector(HttpClient.create().followRedirect { _, res -> res.status().code() in 300..399 }))

        return RSSFinder(imageService, builder)
    }
}
