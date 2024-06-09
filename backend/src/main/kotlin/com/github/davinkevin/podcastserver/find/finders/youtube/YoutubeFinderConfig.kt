package com.github.davinkevin.podcastserver.find.finders.youtube

import com.github.davinkevin.podcastserver.extension.restclient.withStringUTF8MessageConverter
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

@Configuration
@Import(ImageServiceConfig::class)
class YoutubeFinderConfig {

    @Bean
    fun youtubeFinder(imageService: ImageService, rcb: RestClient.Builder): YoutubeFinder {
        val builder = rcb
            .clone()
            .withStringUTF8MessageConverter()
            .defaultHeader("User-Agent", "curl/7.64.1")

        return YoutubeFinder(imageService, builder)
    }

}
