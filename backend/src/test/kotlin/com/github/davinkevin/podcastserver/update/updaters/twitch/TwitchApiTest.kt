package com.github.davinkevin.podcastserver.update.updaters.twitch

import com.github.davinkevin.podcastserver.update.updaters.youtube.YoutubeApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

/**
 * Created by kevin on 17/09/2026
 */
class TwitchApiTest {

    @Configuration
    @EnableConfigurationProperties(TwitchApi::class, YoutubeApi::class)
    class LocalTestConfiguration

    private val context = ApplicationContextRunner()
        .withUserConfiguration(LocalTestConfiguration::class.java)

    private fun AssertableApplicationContext.twitch() = getBean(TwitchApi::class.java)

    @Test
    fun `should have empty default values`() {
        /* Given */
        /* When */
        context
            /* Then */
            .run {
                assertThat(it).hasSingleBean(TwitchApi::class.java)
                assertThat(it.twitch().clientId).isEqualTo("")
                assertThat(it.twitch().clientSecret).isEqualTo("")
                assertThat(it.twitch().videoTypes).containsExactly("all")
            }
    }

    @Test
    fun `should have values provided from configuration`() {
        /* Given */
        /* When */
        context
            .withPropertyValues(
                "podcastserver.api.twitch.client-id=an-id",
                "podcastserver.api.twitch.client-secret=a-secret",
            )
            /* Then */
            .run {
                assertThat(it.twitch().clientId).isEqualTo("an-id")
                assertThat(it.twitch().clientSecret).isEqualTo("a-secret")
            }
    }

    @Test
    fun `should not break the binding of the youtube api key`() {
        /* Given */
        /* When */
        context
            .withPropertyValues(
                "podcastserver.api.youtube=foo",
                "podcastserver.api.twitch.client-id=an-id",
                "podcastserver.api.twitch.client-secret=a-secret",
                "podcastserver.api.twitch.video-types=highlight,upload",
            )
            /* Then */
            .run {
                assertThat(it).hasSingleBean(YoutubeApi::class.java)
                assertThat(it.getBean(YoutubeApi::class.java).youtube).isEqualTo("foo")
                assertThat(it.twitch().videoTypes).containsExactly("highlight", "upload")
            }
    }

    @Nested
    @DisplayName("should convert video types to helix calls")
    inner class ShouldConvertVideoTypesToHelixCalls {

        @Test
        fun `with all, by not sending any type parameter`() {
            /* Given */
            /* When */
            context.withPropertyValues("podcastserver.api.twitch.video-types=all")
                /* Then */
                .run { assertThat(it.twitch().helixVideoTypes).containsExactly(null) }
        }

        @Test
        fun `with a single type, by sending that type`() {
            /* Given */
            /* When */
            context.withPropertyValues("podcastserver.api.twitch.video-types=highlight")
                /* Then */
                .run { assertThat(it.twitch().helixVideoTypes).containsExactly("highlight") }
        }

        @Test
        fun `with several types, by sending one call per type`() {
            /* Given */
            /* When */
            context.withPropertyValues("podcastserver.api.twitch.video-types=highlight,upload")
                /* Then */
                .run { assertThat(it.twitch().helixVideoTypes).containsExactly("highlight", "upload") }
        }

        @Test
        fun `with all combined with others, by collapsing to all`() {
            /* Given */
            /* When */
            context.withPropertyValues("podcastserver.api.twitch.video-types=highlight,all,upload")
                /* Then */
                .run { assertThat(it.twitch().helixVideoTypes).containsExactly(null) }
        }
    }

    @Test
    fun `should reject an unknown video type at startup`() {
        /* Given */
        /* When */
        context.withPropertyValues("podcastserver.api.twitch.video-types=highlight,vod")
            /* Then */
            .run {
                assertThat(it).hasFailed()
                assertThat(it.startupFailure)
                    .rootCause()
                    .hasMessageContaining("invalid value(s) vod for podcastserver.api.twitch.video-types")
            }
    }
}
