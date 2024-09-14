package com.github.davinkevin.podcastserver.download.downloaders.ffmpeg

import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelperFactory
import com.github.davinkevin.podcastserver.download.downloaders.rtmp.RTMPDownloaderFactory
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.ffmpeg.FfmpegService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean

class FfmepgDownloaderConfigTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(LocalTestConfiguration::class.java, FfmepgDownloaderConfig::class.java))

    @Test
    fun `should provide a rtmp downloader factory`() {
        /* Given */
        /* When */
        contextRunner.run {
            /* Then */
            assertThat(it).hasSingleBean(FfmpegDownloaderFactory::class.java)
        }
    }
}

private class LocalTestConfiguration {
    @Bean fun helperFactory(): DownloaderHelperFactory = mock()
    @Bean fun ffmpegService(): FfmpegService = mock()
    @Bean fun processService(): ProcessService = mock()
}
