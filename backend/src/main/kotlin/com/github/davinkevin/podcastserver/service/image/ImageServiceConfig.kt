package com.github.davinkevin.podcastserver.service.image

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class ImageServiceConfig {

    @Bean
    fun imageServiceV2(wcb: WebClient.Builder): ImageService {
        val builder = wcb
                .clone()
                .clientConnector(ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))

        return ImageService(builder)
    }
}
