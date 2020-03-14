package com.github.davinkevin.podcastserver.find.finders.francetv

import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import com.github.davinkevin.podcastserver.service.image.ImageService

/**
 * Created by kevin on 01/11/2019
 */
@Configuration
@Import(ImageServiceConfig::class)
class FranceTvFinderConfig {

    @Bean
    fun franceTvFinder(wcb: WebClient.Builder, imageService: ImageService): FranceTvFinder {
        val client = wcb.clone()
                .clientConnector(ReactorClientHttpConnector(HttpClient.create().followRedirect { _, res -> res.status().code() in 300..399 }))
                .baseUrl("https://www.france.tv/")
                .build()

        return FranceTvFinder(client, imageService)
    }
}
