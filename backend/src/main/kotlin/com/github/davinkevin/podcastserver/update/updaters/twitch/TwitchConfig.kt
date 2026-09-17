package com.github.davinkevin.podcastserver.update.updaters.twitch

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.time.Clock

/**
 * Everything the twitch finder and the twitch updater share: the credentials, the
 * app access token and the health indicator reporting on it.
 *
 * Imported by both `TwitchFinderConfig` and `TwitchUpdaterConfig`, so a single token
 * provider serves the whole application.
 */
@Configuration
@Conditional(TwitchCredentialsCondition::class)
@EnableConfigurationProperties(TwitchApi::class)
class TwitchConfig {

    @Bean
    fun twitchTokenProvider(api: TwitchApi, rcb: RestClient.Builder, clock: Clock): TwitchTokenProvider {
        val rc = rcb
            .clone()
            .baseUrl("https://id.twitch.tv")
            .build()

        return TwitchTokenProvider(
            rc = rc,
            clientId = api.clientId,
            clientSecret = api.clientSecret,
            clock = clock,
        )
    }

    /** bean name is the health indicator id reported by `/actuator/health` */
    @Bean
    fun twitch(token: TwitchTokenProvider): TwitchHealthIndicator = TwitchHealthIndicator(token)
}

internal fun helixClient(rcb: RestClient.Builder, api: TwitchApi): RestClient = rcb
    .clone()
    .baseUrl("https://api.twitch.tv")
    .defaultHeader("Client-Id", api.clientId)
    .build()
