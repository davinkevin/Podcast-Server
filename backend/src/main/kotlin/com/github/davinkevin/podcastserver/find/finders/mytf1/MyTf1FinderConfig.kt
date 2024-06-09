package com.github.davinkevin.podcastserver.find.finders.mytf1

import com.github.davinkevin.podcastserver.extension.restclient.withStringUTF8MessageConverter
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

/**
 * Created by kevin on 12/01/2020
 */
@Configuration
@Import(ImageServiceConfig::class)
class MyTf1FinderConfig {

    @Bean
    fun myTf1Finder(
        rcb: RestClient.Builder,
        image: ImageService
    ): MyTf1Finder {
        val client = rcb.clone()
            .baseUrl("https://www.tf1.fr/")
            .withStringUTF8MessageConverter()
            .build()

        return MyTf1Finder(client, image)
    }

}
