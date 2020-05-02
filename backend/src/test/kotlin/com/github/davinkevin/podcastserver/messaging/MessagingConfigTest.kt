package com.github.davinkevin.podcastserver.messaging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * Created by kevin on 03/05/2020
 */
class MessagingConfigTest {

    private val contextRunner = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MessagingConfig::class.java,
                    JacksonAutoConfiguration::class.java,
                    WebClientAutoConfiguration::class.java
            ))

    @Nested
    @DisplayName("should load sync client")
    inner class ShouldLoadSyncClient {

        @Test
        fun `if both local and dns values are defined`() {
            /* Given */
            /* When */
            contextRunner
                    .withPropertyValues(
                            """podcastserver.cluster.dns=dns""",
                            """podcastserver.cluster.local=local"""
                    )
                    /* Then */
                    .run {
                        assertThat(it).hasSingleBean(MessageSyncClient::class.java)
                    }
        }

    }

    @Nested
    @DisplayName("should not load sync client")
    inner class ShouldNotLoadSyncClient {
        @Test
        fun `if podcastserver-cluster-local is not defined`() {
            /* Given */
            /* When */
            contextRunner
                    .withPropertyValues(
                            """podcastserver.cluster.dns=dns"""
                    )
                    /* Then */
                    .run {
                        assertThat(it).doesNotHaveBean(MessageSyncClient::class.java)
                    }
        }

        @Test
        fun `if podcastserver-cluster-local is empty`() {
            /* Given */
            /* When */
            contextRunner
                    .withPropertyValues(
                            """podcastserver.cluster.local=""",
                            """podcastserver.cluster.dns=dns"""
                    )
                    /* Then */
                    .run {
                        assertThat(it).doesNotHaveBean(MessageSyncClient::class.java)
                    }
        }

        @Test
        fun `if podcastserver-cluster-dns is not defined`() {
            /* Given */
            /* When */
            contextRunner
                    .withPropertyValues(
                            """podcastserver.cluster.local=local"""
                    )
                    /* Then */
                    .run {
                        assertThat(it).doesNotHaveBean(MessageSyncClient::class.java)
                    }
        }

        @Test
        fun `if podcastserver-cluster-dns is empty`() {
            /* Given */
            /* When */
            contextRunner
                    .withPropertyValues(
                            """podcastserver.cluster.dns=""",
                            """podcastserver.cluster.local=local"""
                    )
                    /* Then */
                    .run {
                        assertThat(it).doesNotHaveBean(MessageSyncClient::class.java)
                    }
        }

        @Test
        fun `if both podcastserver-cluster-dns and podcastserver-cluster-local are empty`() {
            /* Given */
            /* When */
            contextRunner
                    .withPropertyValues(
                            """podcastserver.cluster.dns=""",
                            """podcastserver.cluster.local="""
                    )
                    /* Then */
                    .run {
                        assertThat(it).doesNotHaveBean(MessageSyncClient::class.java)
                    }
        }

        @Test
        fun `if both podcastserver-cluster-dns and podcastserver-cluster-local are not defined`() {
            /* Given */
            /* When */
            contextRunner
                    /* Then */
                    .run {
                        assertThat(it).doesNotHaveBean(MessageSyncClient::class.java)
                    }
        }
    }

}
