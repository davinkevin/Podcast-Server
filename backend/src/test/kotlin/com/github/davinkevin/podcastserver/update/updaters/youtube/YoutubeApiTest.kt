package com.github.davinkevin.podcastserver.update.updaters.youtube

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient

/**
 * Created by kevin on 13/04/2016 for Podcast Server
 */
class YoutubeApiTest {

    private val context = ApplicationContextRunner()
        .withBean(SimpleMeterRegistry::class.java)
        .withUserConfiguration(YoutubeUpdaterConfig::class.java)

    @Test
    fun `should have an empty default value for youtube api key`() {
        /* Given */

        /* When */
        context
            .withBean(RestClient.Builder::class.java, { RestClient.builder() })
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
            .withBean(RestClient.Builder::class.java, { RestClient.builder() })
            .withBean(WebClient.Builder::class.java, { WebClient.builder() })
            /* Then */
            .run {
                assertThat(it).hasSingleBean(YoutubeApi::class.java)
                assertThat(it.getBean(YoutubeApi::class.java).youtube).isEqualTo("foo")
            }
    }
}
