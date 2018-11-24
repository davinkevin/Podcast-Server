package com.github.davinkevin.podcastserver.config

import com.github.davinkevin.podcastserver.utils.custom.ffmpeg.CustomRunProcessFunc
import com.nhaarman.mockitokotlin2.mock
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFprobe
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
        val ffmpeg = mock<FFmpeg>()
        val ffprobe = mock<FFprobe>()

        /* When */
        val ffmpegExecutor = ffmpegConfig.ffmpegExecutor(ffmpeg, ffprobe)

        /* Then */
        assertThat(ffmpegExecutor).isNotNull()
    }

    @Test
    fun `should generate ffmpeg`() {
        /* Given */
        val binary = "/bin/bash"

        /* When */
        val ffmpeg = ffmpegConfig.ffmpeg(binary, CustomRunProcessFunc())

        /* Then */
        assertThat(ffmpeg).isNotNull()
    }

    @Test
    fun `should generate ffprobe`() {
        /* Given */
        val binary = "/bin/bash"

        /* When */
        val ffprobe = ffmpegConfig.ffprobe(binary, CustomRunProcessFunc())

        /* Then */
        assertThat(ffprobe).isNotNull()
    }

    @Test
    fun `should have a run process func`() {
        assertThat(ffmpegConfig.runProcessFunc()).isNotNull()
    }
}
