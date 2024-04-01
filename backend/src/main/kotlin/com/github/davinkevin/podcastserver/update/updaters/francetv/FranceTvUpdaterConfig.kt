package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient
import com.github.davinkevin.podcastserver.service.image.ImageService
import java.time.Clock

/**
 * Created by kevin on 18/02/2020
 */
@Configuration
@Import(ImageServiceConfig::class)
class FranceTvUpdaterConfig {

    @Bean
    fun franceTvUpdater(
            wcb: WebClient.Builder,
            image: ImageService,
            mapper: ObjectMapper,
            clock: Clock
    ): FranceTvUpdater {
        val franceTvClient = wcb.clone().baseUrl("https://www.france.tv/").build()

        return FranceTvUpdater(franceTvClient, image, mapper, clock)
    }

}
