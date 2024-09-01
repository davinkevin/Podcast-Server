package com.github.davinkevin.podcastserver.download.downloaders.rtmp

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(RTMPDownloaderFactory::class)
class RTMPDownloaderConfig