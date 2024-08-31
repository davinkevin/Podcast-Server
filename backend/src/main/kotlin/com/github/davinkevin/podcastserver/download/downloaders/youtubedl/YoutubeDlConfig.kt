package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.gitlab.davinkevin.podcastserver.youtubedl.YoutubeDL
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Created by kevin on 08/05/2020
 */
@Configuration
@EnableConfigurationProperties(ExternalTools::class, PodcastServerParameters::class)
@Import(YoutubeDlDownloaderFactory::class)
class YoutubeDlConfig {
    @Bean
    fun youtubeDlService(externalTools: ExternalTools): YoutubeDlService {
        val youtube = YoutubeDL(externalTools.youtubedl)
        return YoutubeDlService(youtube)
    }
}
