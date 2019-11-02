package com.github.davinkevin.podcastserver.find.finders.sixplay

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import com.jayway.jsonpath.Configuration.*
import com.jayway.jsonpath.JsonPath.using
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

@Configuration
@Import(ImageServiceConfig::class)
class SixPlayFinderConfig {

    @Bean
    fun sixPlayFinder(
            wcb: WebClient.Builder,
            image: ImageService,
            mapper: ObjectMapper
    ): SixPlayFinder {
        val client = wcb.clone()
                .baseUrl("https://www.6play.fr")
                .build()

        return SixPlayFinder(client, image, mapper)
    }
}
