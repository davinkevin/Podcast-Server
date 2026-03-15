package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import org.springframework.boot.context.properties.ConfigurationProperties

enum class AutomaticDownload { ENABLED, DISABLED }

@ConfigurationProperties("podcastserver.downloader.youtube")
data class YoutubeDownloaderProperties(
    val automaticDownload: AutomaticDownload = AutomaticDownload.ENABLED,
)
