package com.github.davinkevin.podcastserver.update.updaters.youtube

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.web.reactive.function.client.WebClient

/**
 * Created by kevin on 13/04/2016 for Podcast Server
 */
class YoutubeApiTest {

    private val context = ApplicationContextRunner()
        .withUserConfiguration(YoutubeUpdaterConfig::class.java)

    @Test
    fun `should have an empty default value for youtube api key`() {
        /* Given */

        /* When */
        context
            .withBean(WebClient.Builder::class.java, { WebClient.builder() })
            /* Then */
            .run {
                assertThat(it).hasSingleBean(YoutubeApi::class.java)
                assertThat(it.getBean(YoutubeApi::class.java).youtube).isEqualTo("")
            }
    }

    @Test
    fun `should have value provided from configuration`() {
        /* Given */

        /* When */
        context
            .withPropertyValues(
                "podcastserver.api.youtube=foo"
            )
            .withBean(WebClient.Builder::class.java, { WebClient.builder() })
            /* Then */
            .run {
                assertThat(it).hasSingleBean(YoutubeApi::class.java)
                assertThat(it.getBean(YoutubeApi::class.java).youtube).isEqualTo("foo")
            }
    }
}
