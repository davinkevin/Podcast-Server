package com.github.davinkevin.podcastserver.update.updaters.twitch

import com.github.davinkevin.podcastserver.find.finders.twitch.TwitchFinder
import com.github.davinkevin.podcastserver.find.finders.twitch.TwitchFinderConfig
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.web.client.RestClient
import java.time.Clock

/**
 * Created by kevin on 17/09/2026
 */
class TwitchConfigTest {

    private val context = ApplicationContextRunner()
        .withBean(RestClient.Builder::class.java, { RestClient.builder() })
        .withBean(Clock::class.java, { Clock.systemUTC() })
        .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
        .withUserConfiguration(TwitchUpdaterConfig::class.java, TwitchFinderConfig::class.java)

    @Nested
    @DisplayName("should register no bean at all")
    inner class ShouldRegisterNoBeanAtAll {

        @Test
        fun `without any credential`() {
            /* Given */
            /* When */
            context
                /* Then */
                .run {
                    assertThat(it).doesNotHaveBean(TwitchUpdater::class.java)
                    assertThat(it).doesNotHaveBean(TwitchFinder::class.java)
                    assertThat(it).doesNotHaveBean(TwitchTokenProvider::class.java)
                    assertThat(it).doesNotHaveBean(TwitchHealthIndicator::class.java)
                }
        }

        @Test
        fun `with a missing client secret`() {
            /* Given */
            /* When */
            context.withPropertyValues("podcastserver.api.twitch.client-id=an-id")
                /* Then */
                .run { assertThat(it).doesNotHaveBean(TwitchUpdater::class.java) }
        }

        @Test
        fun `with a blank credential`() {
            /* Given */
            /* When */
            context
                .withPropertyValues(
                    "podcastserver.api.twitch.client-id=an-id",
                    "podcastserver.api.twitch.client-secret=   ",
                )
                /* Then */
                .run {
                    assertThat(it).doesNotHaveBean(TwitchUpdater::class.java)
                    assertThat(it).doesNotHaveBean(TwitchFinder::class.java)
                }
        }
    }

    @Test
    fun `should register every twitch bean once both credentials are set`() {
        /* Given */
        /* When */
        context
            .withPropertyValues(
                "podcastserver.api.twitch.client-id=an-id",
                "podcastserver.api.twitch.client-secret=a-secret",
            )
            /* Then */
            .run {
                assertThat(it).hasSingleBean(TwitchUpdater::class.java)
                assertThat(it).hasSingleBean(TwitchFinder::class.java)
                assertThat(it).hasSingleBean(TwitchTokenProvider::class.java)
                assertThat(it).hasSingleBean(TwitchHealthIndicator::class.java)
                assertThat(it.getBeanNamesForType(TwitchHealthIndicator::class.java)).containsExactly("twitch")
            }
    }
}
