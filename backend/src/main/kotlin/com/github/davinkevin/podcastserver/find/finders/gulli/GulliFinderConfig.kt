package com.github.davinkevin.podcastserver.find.finders.gulli

import com.github.davinkevin.podcastserver.extension.restclient.withStringUTF8MessageConverter
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

@Configuration
@Import(ImageServiceConfig::class)
class GulliFinderConfig {

    @Bean
    fun gulliFinder(rcb: RestClient.Builder, image: ImageService): GulliFinder {
        val client = rcb
                .clone()
                .baseUrl("https://replay.gulli.fr")
                .withStringUTF8MessageConverter()
                .build()

        return GulliFinder(client, image)
    }

}
