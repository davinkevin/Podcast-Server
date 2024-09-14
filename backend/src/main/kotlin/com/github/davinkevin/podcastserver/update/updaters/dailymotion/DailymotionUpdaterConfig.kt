package com.github.davinkevin.podcastserver.update.updaters.dailymotion

import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

@Configuration
@Import(ImageServiceConfig::class)
class DailymotionUpdaterConfig {

    @Bean
    fun dailymotionUpdater(
        rcb: RestClient.Builder,
        image: ImageService,
        registry: MeterRegistry,
    ): DailymotionUpdater {
        val rc = rcb.clone().baseUrl("https://api.dailymotion.com").build()

        return DailymotionUpdater(rc, image, registry)
    }
}
