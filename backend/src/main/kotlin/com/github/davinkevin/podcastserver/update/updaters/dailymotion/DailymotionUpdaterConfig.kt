package com.github.davinkevin.podcastserver.update.updaters.dailymotion

import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient

/**
 * Created by kevin on 13/03/2020
 */
@Configuration
@Import(ImageServiceConfig::class)
class DailymotionUpdaterConfig {

    @Bean
    fun dailymotionUpdater(
            wcb: WebClient.Builder,
            image: ImageServiceV2
    ): DailymotionUpdater {
        val wc = wcb.baseUrl("https://api.dailymotion.com").build()

        return DailymotionUpdater(wc, image)
    }
}
