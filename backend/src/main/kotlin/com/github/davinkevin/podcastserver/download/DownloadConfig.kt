package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.reactive.function.server.router

/**
 * Created by kevin on 17/09/2019
 */
@Configuration
@Import(DownloadHandler::class)
class DownloadRouterConfig {

    @Bean
    fun downloadRouter(d: DownloadHandler) = router {
        POST("/api/v1/podcasts/{idPodcast}/items/{id}/download", d::download)

        "/api/v1/downloads".nest {
            GET("/downloading", d::downloading)

            "/limit".nest {
                GET("", d::findLimit)
                POST("", d::updateLimit)
            }

            POST("/stop", d::stopAll)

            "/{id}".nest {
                POST("/stop", d::stopOne)
            }

            "/queue".nest {
                GET("", d::queue)
                POST("", d::moveInQueue)
                DELETE("/{id}", d::removeFromQueue)
            }
        }
    }

}

@Configuration
@Import(
        DownloadRouterConfig::class,
        ItemDownloadManager::class,
        DownloadRepository::class
)
class DownloadConfig {

    private val log = LoggerFactory.getLogger(DownloadConfig::class.java)

    @Bean
    fun downloadExecutor(
        parameters: PodcastServerParameters,
        @Value("\${spring.threads.virtual.enabled:false}") isVirtualThreadEnabled: Boolean
    ) = ThreadPoolTaskExecutor().apply {
        if (isVirtualThreadEnabled) {
            log.info("Downloader thread pool is based on virtual threads")
            newThread(Thread::ofVirtual)
        }
        corePoolSize = parameters.concurrentDownload
        setThreadNamePrefix("Downloader-")
        initialize()
    }

    @Bean
    fun onStartupCleanInvalidDownloadingItemsState(download: DownloadRepository) = CommandLineRunner {
        download
            .resetToWaitingStateAllDownloadingItems()
            .block()
    }
}
