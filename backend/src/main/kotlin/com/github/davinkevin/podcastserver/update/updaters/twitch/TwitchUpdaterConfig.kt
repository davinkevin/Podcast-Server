package com.github.davinkevin.podcastserver.update.updaters.twitch

import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

@Configuration
@Conditional(TwitchCredentialsCondition::class)
@Import(TwitchConfig::class, ImageServiceConfig::class)
class TwitchUpdaterConfig {

    @Bean
    fun twitchUpdater(
        api: TwitchApi,
        rcb: RestClient.Builder,
        token: TwitchTokenProvider,
        image: ImageService,
        registry: MeterRegistry,
    ): TwitchUpdater = TwitchUpdater(
        rc = helixClient(rcb, api),
        token = token,
        image = image,
        videoTypes = api.helixVideoTypes,
        registry = registry,
    )
}
