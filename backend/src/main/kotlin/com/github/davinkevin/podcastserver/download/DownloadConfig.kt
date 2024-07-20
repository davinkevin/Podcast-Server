package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.servlet.function.router

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

    @Bean
    fun downloadExecutor(parameters: PodcastServerParameters) = ThreadPoolTaskExecutor().apply {
        newThread(Thread::ofVirtual)
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
