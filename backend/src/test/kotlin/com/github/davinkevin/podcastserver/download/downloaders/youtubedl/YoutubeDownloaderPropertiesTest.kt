package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class YoutubeDownloaderPropertiesTest {

    @Test
    fun `should have automatic download enabled by default`() {
        val properties = YoutubeDownloaderProperties()

        assertThat(properties.automaticDownload).isEqualTo(AutomaticDownload.ENABLED)
    }

    @Test
    fun `should have automatic download disabled when configured`() {
        val properties = YoutubeDownloaderProperties(automaticDownload = AutomaticDownload.DISABLED)

        assertThat(properties.automaticDownload).isEqualTo(AutomaticDownload.DISABLED)
    }

    @Nested
    @DisplayName("should bind from Spring configuration")
    inner class ShouldBindFromSpringConfiguration {

        private val contextRunner = ApplicationContextRunner()
            .withUserConfiguration(EnableYoutubeDownloaderPropertiesConfiguration::class.java)

        @Test
        fun `with default values`() {
            contextRunner.run {
                val properties = it.getBean(YoutubeDownloaderProperties::class.java)
                assertThat(properties.automaticDownload).isEqualTo(AutomaticDownload.ENABLED)
            }
        }

        @Test
        fun `with automatic download disabled`() {
            contextRunner
                .withPropertyValues("podcastserver.downloader.youtube.automatic-download=DISABLED")
                .run {
                    val properties = it.getBean(YoutubeDownloaderProperties::class.java)
                    assertThat(properties.automaticDownload).isEqualTo(AutomaticDownload.DISABLED)
                }
        }

        @Test
        fun `with automatic download enabled`() {
            contextRunner
                .withPropertyValues("podcastserver.downloader.youtube.automatic-download=ENABLED")
                .run {
                    val properties = it.getBean(YoutubeDownloaderProperties::class.java)
                    assertThat(properties.automaticDownload).isEqualTo(AutomaticDownload.ENABLED)
                }
        }
    }
}

@EnableConfigurationProperties(YoutubeDownloaderProperties::class)
private class EnableYoutubeDownloaderPropertiesConfiguration
