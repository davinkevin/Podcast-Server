package com.github.davinkevin.podcastserver.service.ffmpeg

import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Created by kevin on 13/06/2016 for Podcast Server
 */
class FfmpegConfigTest {

    private val ffmpegConfig = FfmpegConfig()

    @Test
    fun `should have a bean for executor`() {
        /* Given */
        val externalTools = ExternalTools(
            ffmpeg = "/bin/echo",
            ffprobe = "/bin/echo"
        )

        /* When */
        val service = ffmpegConfig.ffmpegService(externalTools)

        /* Then */
        assertThat(service).isNotNull()
    }
}
