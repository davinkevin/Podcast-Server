package com.github.davinkevin.podcastserver.service.ffmpeg

import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import com.github.davinkevin.podcastserver.utils.custom.ffmpeg.CustomRunProcessFunc
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Created by kevin on 21/05/2016 for Podcast Server
 */
@Configuration
@EnableConfigurationProperties(ExternalTools::class)
class FfmpegConfig {

    @Bean
    fun ffmpegService(externalTools: ExternalTools): FfmpegService {
        val processFunc = CustomRunProcessFunc()

        val ffmpeg = FFmpeg(externalTools.ffmpeg, processFunc)
        val ffprobe = FFprobe(externalTools.ffprobe, processFunc)

        val executor = FFmpegExecutor(ffmpeg, ffprobe)

        return FfmpegService(processFunc, executor, ffprobe)
    }

}
