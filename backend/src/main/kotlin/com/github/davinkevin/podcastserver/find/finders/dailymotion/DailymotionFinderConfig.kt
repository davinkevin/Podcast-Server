package com.github.davinkevin.podcastserver.find.finders.dailymotion

import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

/**
 * Created by kevin on 01/11/2019
 */
@Configuration
@Import(ImageServiceConfig::class)
class DailymotionFinderConfig {

    @Bean
    fun dailymotionFinder(rcb: RestClient.Builder, image: ImageService): DailymotionFinder {
        val client = rcb
                .clone()
                .baseUrl("https://api.dailymotion.com/")
                .build()

        return DailymotionFinder(client, image)
    }
}
