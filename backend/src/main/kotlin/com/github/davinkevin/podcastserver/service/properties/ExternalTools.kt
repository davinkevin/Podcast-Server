package com.github.davinkevin.podcastserver.service.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Created by kevin on 12/04/2016 for Podcast Server
 */
@ConfigurationProperties("podcastserver.externaltools")
class ExternalTools(
        var ffmpeg: String = "/usr/local/bin/ffmpeg",
        var rtmpdump: String = "/usr/local/bin/rtmpdump"
)
