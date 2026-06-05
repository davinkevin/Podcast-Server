package com.github.davinkevin.podcastserver.download.downloaders

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
        return downloaderFactories.minByOrNull { it.compatibility(information) }!!
    }
}
