package com.github.davinkevin.podcastserver.service.image

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class ImageServiceConfig {

    @Bean
    fun imageServiceV2(wcb: RestClient.Builder): ImageService = ImageService(wcb.clone())
}
