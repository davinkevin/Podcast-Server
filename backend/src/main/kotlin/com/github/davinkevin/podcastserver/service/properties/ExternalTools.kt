package com.github.davinkevin.podcastserver.service.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Created by kevin on 12/04/2016 for Podcast Server
 */
@ConfigurationProperties("podcastserver.externaltools")
data class ExternalTools(
        val ffmpeg: String = "/usr/local/bin/ffmpeg",
        val ffprobe: String = "/usr/local/bin/ffprobe",
        val rtmpdump: String = "/usr/local/bin/rtmpdump",
        val youtubedl: String = "/usr/local/bin/youtube-dl"
)
