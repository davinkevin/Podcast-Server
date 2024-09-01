package com.github.davinkevin.podcastserver.download.downloaders

import com.github.davinkevin.podcastserver.download.downloaders.noop.NoOpDownloader
import com.github.davinkevin.podcastserver.download.downloaders.noop.NoOpDownloaderFactory
import org.springframework.aop.TargetClassAware
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

/**
 * Created by kevin on 17/03/15.
 */
@Service
class DownloaderSelector(
    val context: ApplicationContext,
    val downloaderFactories: Set<DownloaderFactory>
) {

    @Suppress("UNCHECKED_CAST")
    fun of(information: DownloadingInformation): DownloaderFactory {
        if (information.urls.isEmpty()) {
            return NoOpDownloaderFactory
        }

        val d = downloaderFactories.minByOrNull { it.compatibility(information) }!!
        if (d is Downloader) {
            val clazz = (if (d is TargetClassAware) d.targetClass else d.javaClass) as Class<Downloader>
            return context.getBean(clazz)
        }
        return d
    }

    companion object {
        val NoOpDownloaderFactory = NoOpDownloaderFactory()
    }
}
