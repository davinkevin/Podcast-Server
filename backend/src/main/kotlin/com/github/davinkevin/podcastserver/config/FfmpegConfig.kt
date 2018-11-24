package com.github.davinkevin.podcastserver.config

import com.github.davinkevin.podcastserver.utils.custom.ffmpeg.CustomRunProcessFunc
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Created by kevin on 21/05/2016 for Podcast Server
 */
@Configuration
class FfmpegConfig {

    @Bean
    fun ffmpegExecutor(ffmpeg: FFmpeg, ffprobe: FFprobe) = FFmpegExecutor(ffmpeg, ffprobe)

    @Bean
    fun ffmpeg(
            @Value("\${podcastserver.externaltools.ffmpeg:/usr/local/bin/ffmpeg}") ffmpegLocation: String,
            runProcessFunc: CustomRunProcessFunc
    ) =
            FFmpeg(ffmpegLocation, runProcessFunc)

    @Bean
    fun ffprobe(
            @Value("\${podcastserver.externaltools.ffprobe:/usr/local/bin/ffprobe}") ffprobeLocation: String,
            runProcessFunc: CustomRunProcessFunc
    ) =
            FFprobe(ffprobeLocation, runProcessFunc)

    @Bean
    fun runProcessFunc(): CustomRunProcessFunc = CustomRunProcessFunc()

}
