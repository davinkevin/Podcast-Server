package com.github.davinkevin.podcastserver.find.finders.gulli

import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient
import com.github.davinkevin.podcastserver.service.image.ImageService

@Configuration
@Import(ImageServiceConfig::class)
class GulliFinderConfig {

    @Bean
    fun gulliFinder(wcb: WebClient.Builder, image: ImageService): GulliFinder {
        val client = wcb
                .clone()
                .baseUrl("https://replay.gulli.fr")
                .build()

        return GulliFinder(client, image)
    }

}
