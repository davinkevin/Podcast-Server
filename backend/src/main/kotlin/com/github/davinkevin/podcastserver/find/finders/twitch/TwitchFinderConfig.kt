package com.github.davinkevin.podcastserver.find.finders.twitch

import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.image.ImageServiceConfig
import com.github.davinkevin.podcastserver.update.updaters.twitch.TwitchApi
import com.github.davinkevin.podcastserver.update.updaters.twitch.TwitchConfig
import com.github.davinkevin.podcastserver.update.updaters.twitch.TwitchCredentialsCondition
import com.github.davinkevin.podcastserver.update.updaters.twitch.TwitchTokenProvider
import com.github.davinkevin.podcastserver.update.updaters.twitch.helixClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

@Configuration
@Conditional(TwitchCredentialsCondition::class)
@Import(TwitchConfig::class, ImageServiceConfig::class)
class TwitchFinderConfig {

    @Bean
    fun twitchFinder(
        api: TwitchApi,
        rcb: RestClient.Builder,
        token: TwitchTokenProvider,
        imageService: ImageService,
    ): TwitchFinder = TwitchFinder(helixClient(rcb, api), token, imageService)
}
