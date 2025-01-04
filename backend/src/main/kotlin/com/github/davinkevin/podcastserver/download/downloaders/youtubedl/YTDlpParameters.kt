package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import org.springframework.boot.context.properties.ConfigurationProperties

// https://github.com/yt-dlp/yt-dlp?tab=readme-ov-file#general-options
@ConfigurationProperties("podcastserver.external-tools.yt-dlp")
data class YTDlpParameters(
    val path: String = "/usr/local/bin/youtube-dl",
    val extraParameters: String = "{}",
)