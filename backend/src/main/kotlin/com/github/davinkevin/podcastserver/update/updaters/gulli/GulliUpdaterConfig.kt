package com.github.davinkevin.podcastserver.update.updaters.gulli

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import com.github.davinkevin.podcastserver.service.image.ImageService
import org.springframework.web.client.RestClient

/**
 * Created by kevin on 14/03/2020
 */
@Configuration
class GulliUpdaterConfig {

    @Bean
    fun gulliUpdater(rcb: RestClient.Builder, image: ImageService, mapper: ObjectMapper): GulliUpdater {
        val wc = rcb.clone()
                .baseUrl("https://replay.gulli.fr")
                .build()

        return GulliUpdater(wc, image, mapper)
    }
}
