package com.github.davinkevin.podcastserver.find.finders.francetv

import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

/**
 * Created by kevin on 01/11/2019
 */
@Configuration
@Import(ImageServiceConfig::class)
class FranceTvFinderConfig {

    @Bean
    fun franceTvFinder(rcb: RestClient.Builder, imageService: ImageService): FranceTvFinder {
        val client = rcb.clone()
                .baseUrl("https://www.france.tv/")
                .build()

        return FranceTvFinder(client, imageService)
    }
}
