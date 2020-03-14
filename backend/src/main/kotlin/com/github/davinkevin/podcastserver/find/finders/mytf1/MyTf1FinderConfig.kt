package com.github.davinkevin.podcastserver.find.finders.mytf1

import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient
import com.github.davinkevin.podcastserver.service.image.ImageService

/**
 * Created by kevin on 12/01/2020
 */
@Configuration
@Import(ImageServiceConfig::class)
class MyTf1FinderConfig {

    @Bean
    fun myTf1Finder(
            wcb: WebClient.Builder,
            image: ImageService
    ): MyTf1Finder {
        val client = wcb.clone()
                .baseUrl("https://www.tf1.fr/")
                .build()

        return MyTf1Finder(client, image)
    }

}
