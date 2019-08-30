package com.github.davinkevin.podcastserver.find.finders.youtube

import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
@Import(ImageServiceConfig::class)
class YoutubeFinderConfig {

    @Bean
    fun youtubeFinder(imageServiceV2: ImageServiceV2, wcb: WebClient.Builder): YoutubeFinder {
        val builder = wcb
                .clone()
                .clientConnector(ReactorClientHttpConnector(HttpClient.create().followRedirect { _, res -> res.status().code() in 300..399 }))

        return YoutubeFinder(imageServiceV2, builder)
    }

}
