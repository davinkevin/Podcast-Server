package com.github.davinkevin.podcastserver.update.updaters.francetv

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient
import java.time.Clock

/**
 * Created by kevin on 18/02/2020
 */
@Configuration
@Import(ImageServiceConfig::class)
class FranceTvUpdaterConfig {

    @Bean
    fun franceTvUpdater(
        rcb: RestClient.Builder,
        image: ImageService,
        mapper: ObjectMapper,
        clock: Clock,
        registry: MeterRegistry,
    ): FranceTvUpdater {
        val franceTvClient = rcb.clone().baseUrl("https://www.france.tv/").build()

        return FranceTvUpdater(franceTvClient, image, mapper, clock, registry)
    }

}
