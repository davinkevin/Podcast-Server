package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import java.time.Clock

/**
 * Created by kevin on 08/05/2020
 */

class YoutubeDlConfigTest {

    private val contextRunner = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(YoutubeDlConfig::class.java))

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
    fun `should provide a youtube dl downloader`() {
        /* Given */

        /* When */
        contextRunner.run {
            /* Then */
            assertThat(it).hasSingleBean(YoutubeDlDownloader::class.java)
        }
    }

    @Test
    fun `should provide a youtube dl downloader as a prototype`() {
        /* Given */

        /* When */
        contextRunner
                .withConfiguration(AutoConfigurations.of(LocalTestConfiguration::class.java))
                .run {
            /* Then */
            val first = it.getBean(YoutubeDlDownloader::class.java)
            val second = it.getBean(YoutubeDlDownloader::class.java)
            assertThat(first).isNotSameAs(second)
        }
    }
}

private class LocalTestConfiguration {
    @Bean fun downloadRepo(): DownloadRepository = mock()
    @Bean fun template(): MessagingTemplate = mock()
    @Bean fun clock(): Clock = mock()
    @Bean fun file(): FileStorageService = mock()
}
