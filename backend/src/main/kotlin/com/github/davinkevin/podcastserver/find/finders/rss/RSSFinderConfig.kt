package com.github.davinkevin.podcastserver.find.finders.rss

import com.github.davinkevin.podcastserver.extension.restclient.withStringUTF8MessageConverter
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

@Configuration
@Import(ImageServiceConfig::class)
class RSSFinderConfig {

    @Bean
    fun rssFinder(
        imageService: ImageService,
        rcb: RestClient.Builder
    ): RSSFinder {
        val client = rcb.clone()
            .withStringUTF8MessageConverter()

        return RSSFinder(imageService, client)
    }
}
