package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.manager.selector.DownloaderSelector
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * Created by kevin on 28/05/2022
 */
class DownloadConfigTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            DownloadConfig::class.java,
            LocalTestConfiguration::class.java
        ))

    @Test
    fun `should provide downloader repository`() {
        /* Given */
        /* When */
        contextRunner.run {
            /* Then */
            assertThat(it).hasSingleBean(DownloadRepository::class.java)
        }
    }

    @Test
    fun `should provide downloader ItemDownloadManager`() {
        /* Given */
        /* When */
        contextRunner.run {
            /* Then */
            assertThat(it).hasSingleBean(ItemDownloadManager::class.java)
        }
    }

    @Test
    fun `should provide downloader DownloadHandler`() {
        /* Given */
        /* When */
        contextRunner.run {
            /* Then */
            assertThat(it).hasSingleBean(DownloadHandler::class.java)
        }
    }

    @Test
    fun `should generate a thread pool executor`() {
        /* Given */

        /* When */
        contextRunner.run {
            /* Then */
            assertThat(it).hasSingleBean(ThreadPoolTaskExecutor::class.java)
            val pool = it.getBean<ThreadPoolTaskExecutor>()
            assertThat(pool.corePoolSize).isEqualTo(123)
            assertThat(pool.threadNamePrefix).isEqualTo("Downloader-")
        }
    }
}

private class LocalTestConfiguration {
    @Bean fun query(): DSLContext = mock()
    @Bean fun messaging(): MessagingTemplate = mock()
    @Bean fun params(): PodcastServerParameters = mock {
        on { concurrentDownload } doReturn 123
    }
    @Bean fun downloaderSelector(): DownloaderSelector = mock()
}
