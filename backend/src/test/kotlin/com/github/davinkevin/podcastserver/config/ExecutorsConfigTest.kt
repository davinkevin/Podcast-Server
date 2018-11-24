package com.github.davinkevin.podcastserver.config

import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * Created by kevin on 13/08/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class ExecutorsConfigTest {

    @Mock lateinit var podcastServerParameters: PodcastServerParameters
    @InjectMocks lateinit var executorsConfig: ExecutorsConfig

    @Test
    fun `should generate multi thread executor`() {
        /* Given */
        whenever(podcastServerParameters.maxUpdateParallels).thenReturn(10)

        /* When */
        val executor = executorsConfig.updateExecutor()

        /* Then */
        executor.apply {
            assertThat(this).isInstanceOf(ThreadPoolTaskExecutor::class.java)
            assertThat(corePoolSize).isEqualTo(10)
            assertThat(maxPoolSize).isEqualTo(10)
            assertThat(threadNamePrefix).contains("Update")
        }
    }

    @Test
    fun `should generate single thread executor`() {
        /* When */
        val executor = executorsConfig.singleThreadExecutor()

        /* Then */
        executor.apply {
            assertThat(this).isInstanceOf(ThreadPoolTaskExecutor::class.java)
            assertThat(corePoolSize).isEqualTo(1)
            assertThat(maxPoolSize).isEqualTo(1)
            assertThat(threadNamePrefix).contains("Manual")
        }
    }

    @Test
    fun `should generate download thread executor`() {
        /* Given */
        whenever(podcastServerParameters.concurrentDownload).thenReturn(10)

        /* When */
        val executor = executorsConfig.downloadExecutor()

        /* Then */
        executor.apply {
            assertThat(this).isInstanceOf(ThreadPoolTaskExecutor::class.java)
            assertThat(corePoolSize).isEqualTo(10)
            assertThat(threadNamePrefix).contains("Downloader")
        }

    }
}
