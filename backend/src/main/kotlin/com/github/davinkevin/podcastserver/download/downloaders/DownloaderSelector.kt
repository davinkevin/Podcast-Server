package com.github.davinkevin.podcastserver.download.downloaders

import com.github.davinkevin.podcastserver.download.downloaders.noop.NoOpDownloaderFactory
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

    fun of(information: DownloadingInformation): DownloaderFactory {
        if (information.urls.isEmpty()) {
            return NoOpDownloaderFactory
        }

        return downloaderFactories.minByOrNull { it.compatibility(information) }!!
    }

    companion object {
        val NoOpDownloaderFactory = NoOpDownloaderFactory()
    }
}
