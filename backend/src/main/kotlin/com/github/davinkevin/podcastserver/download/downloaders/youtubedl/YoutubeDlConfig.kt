package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.gitlab.davinkevin.podcastserver.youtubedl.YoutubeDL
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

/**
 * Created by kevin on 08/05/2020
 */
@Configuration
@EnableConfigurationProperties(YTDlpParameters::class)
@Import(YoutubeDlDownloaderFactory::class)
class YoutubeDlConfig {

    private val log = LoggerFactory.getLogger(YoutubeDlConfig::class.java)

    @Bean
    fun youtubeDlService(parameters: YTDlpParameters, om: ObjectMapper): YoutubeDlService {
        val youtube = YoutubeDL(parameters.path)
        val extraParameters = om.readValue<Map<String, String>>(parameters.extraParameters)

        log.debug("extra parameters: {}", extraParameters)

        return YoutubeDlService(youtube, extraParameters, parameters.impersonate)
    }
}
