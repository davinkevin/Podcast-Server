package com.github.davinkevin.podcastserver.config

import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * Created by kevin on 08/02/2014.
 */
@Configuration
@EnableAsync
class ExecutorsConfig(val parameters: PodcastServerParameters) {

    @Bean(name = ["UpdateExecutor"])
    fun updateExecutor() = ThreadPoolTaskExecutor().apply {
        corePoolSize = parameters.maxUpdateParallels
        maxPoolSize = parameters.maxUpdateParallels
        setThreadNamePrefix("Update-")
        initialize()
    }

    @Bean(name = ["ManualUpdater"])
    fun singleThreadExecutor() = ThreadPoolTaskExecutor().apply {
        corePoolSize = 1
        maxPoolSize = 1
        setThreadNamePrefix("Manual-Update-")
        initialize()
    }

    @Bean(name = ["DownloadExecutor"])
    fun downloadExecutor() = ThreadPoolTaskExecutor().apply {
        corePoolSize = parameters.concurrentDownload
        setThreadNamePrefix("Downloader-")
        initialize()
    }
}
