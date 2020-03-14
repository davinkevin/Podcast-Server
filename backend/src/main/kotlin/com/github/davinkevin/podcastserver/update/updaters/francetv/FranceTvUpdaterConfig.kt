package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient
import com.github.davinkevin.podcastserver.service.image.ImageService

/**
 * Created by kevin on 18/02/2020
 */
@Configuration
@Import(ImageServiceConfig::class)
class FranceTvUpdaterConfig {

    @Bean
    fun franceTvUpdater(wcb: WebClient.Builder, image: ImageService, mapper: ObjectMapper): FranceTvUpdater {
        val franceTvClient = wcb.clone().baseUrl("https://www.france.tv/").build()
        val franceTvApi = wcb.baseUrl("https://sivideo.webservices.francetelevisions.fr").build()

        return FranceTvUpdater(franceTvClient, franceTvApi, image, mapper)
    }

}
