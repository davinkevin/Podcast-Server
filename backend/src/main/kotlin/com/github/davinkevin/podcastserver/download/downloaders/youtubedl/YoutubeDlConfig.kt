package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import com.gitlab.davinkevin.podcastserver.youtubedl.YoutubeDL
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import java.time.Clock

/**
 * Created by kevin on 08/05/2020
 */
@Configuration
@EnableConfigurationProperties(ExternalTools::class, PodcastServerParameters::class)
class YoutubeDlConfig {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun youtubeDlDownloader(
        downloadRepository: DownloadRepository,
        parameters: PodcastServerParameters,
        template: MessagingTemplate,
        clock: Clock,
        file: FileStorageService,
        youtubeDlService: YoutubeDlService
    ): YoutubeDlDownloader = YoutubeDlDownloader(
        downloadRepository = downloadRepository,
        template = template,
        clock = clock,
        file = file,
        youtubeDl = youtubeDlService
    )

    @Bean
    fun youtubeDlService(externalTools: ExternalTools): YoutubeDlService {
        val youtube = YoutubeDL(externalTools.youtubedl)
        return YoutubeDlService(youtube)
    }


}
