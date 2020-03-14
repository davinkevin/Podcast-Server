package com.github.davinkevin.podcastserver.update.updaters.gulli

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

/**
 * Created by kevin on 14/03/2020
 */
@Configuration
class GulliUpdaterConfig {

    @Bean
    fun gulliUpdater(wcb: WebClient.Builder, image: ImageService, mapper: ObjectMapper): GulliUpdater {
        val wc = wcb.clone()
                .baseUrl("https://replay.gulli.fr")
                .build()

        return GulliUpdater(wc, image, mapper)
    }
}
