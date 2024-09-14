package com.github.davinkevin.podcastserver.update.updaters.mytf1

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode

/**
 * Created by kevin on 11/03/2020
 */
@Configuration
@Import(ImageServiceConfig::class)
class MyTf1UpdaterConfig {

    @Bean
    fun myTf1Updater(
        imageService: ImageService,
        rcb: RestClient.Builder,
        om: ObjectMapper,
        registry: MeterRegistry,
    ): MyTf1Updater {
        val urlBuilderFactory = DefaultUriBuilderFactory("https://www.tf1.fr/").apply {
            encodingMode = EncodingMode.NONE
        }

        val wc = rcb
                .clone()
                .uriBuilderFactory(urlBuilderFactory)
                .build()

        return MyTf1Updater(wc, om, imageService, registry)
    }
}
