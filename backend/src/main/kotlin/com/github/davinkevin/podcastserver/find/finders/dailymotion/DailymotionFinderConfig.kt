package com.github.davinkevin.podcastserver.find.finders.dailymotion

import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

/**
 * Created by kevin on 01/11/2019
 */
@Configuration
@Import(ImageServiceConfig::class)
class DailymotionFinderConfig {

    @Bean
    fun dailymotionFinder(wcb: WebClient.Builder, image: ImageService): DailymotionFinder {
        val client = wcb
                .clone()
                .baseUrl("https://api.dailymotion.com/")
                .build()

        return DailymotionFinder(client, image)
    }
}
