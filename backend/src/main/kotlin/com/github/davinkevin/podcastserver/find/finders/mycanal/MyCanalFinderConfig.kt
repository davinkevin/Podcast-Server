package com.github.davinkevin.podcastserver.find.finders.mycanal

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

/**
 * Created by kevin on 03/11/2019
 */
@Configuration
@Import(ImageServiceConfig::class)
class MyCanalFinderConfig {

    @Bean
    fun myCanalFinder(wcb: WebClient.Builder, image: ImageService, mapper: ObjectMapper): MyCanalFinder {
        val client = wcb.clone()
                .baseUrl("https://www.canalplus.com")
                .build()

        return MyCanalFinder(client, image, mapper)
    }
}
