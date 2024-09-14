package com.github.davinkevin.podcastserver.download.downloaders.ffmpeg

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(FfmpegDownloaderFactory::class)
class FfmpegDownloaderConfig