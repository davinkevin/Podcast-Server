package com.github.davinkevin.podcastserver.update.updaters.rss

import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

@Configuration
@Import(ImageServiceConfig::class)
class RSSUpdaterConfig {

    @Bean
    fun rssUpdater(
        imageService: ImageService,
        rcb: RestClient.Builder,
        registry: MeterRegistry,
    ): RSSUpdater {
        return RSSUpdater(imageService, rcb.clone(), registry)
    }
}
