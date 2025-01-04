package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelperFactory
import com.gitlab.davinkevin.podcastserver.youtubedl.YoutubeDL
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean

/**
 * Created by kevin on 08/05/2020
 */

class YoutubeDlConfigTest {

    private val contextRunner = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                JacksonAutoConfiguration::class.java,
                LocalTestConfiguration::class.java,
                YoutubeDlConfig::class.java)
            )

    @Test
    fun `should provide a youtube dl service`() {
        /* Given */

        /* When */
        contextRunner.run {
            /* Then */
            assertThat(it).hasSingleBean(YoutubeDlService::class.java)
        }
    }

    @Test
    fun `should provide a youtube dl downloader factory`() {
        /* Given */

        /* When */
        contextRunner.run {
            /* Then */
            assertThat(it).hasSingleBean(YoutubeDlDownloaderFactory::class.java)
        }
    }
}

private class LocalTestConfiguration {
    @Bean fun helperFactory(): DownloaderHelperFactory = mock()
    @Bean fun youtubeDL(): YoutubeDL = mock()
}
