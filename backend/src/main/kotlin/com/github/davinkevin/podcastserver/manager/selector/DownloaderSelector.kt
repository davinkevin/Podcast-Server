package com.github.davinkevin.podcastserver.manager.selector

import com.github.davinkevin.podcastserver.manager.downloader.Downloader
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingInformation
import com.github.davinkevin.podcastserver.manager.downloader.NoOpDownloader
import org.springframework.aop.TargetClassAware
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

/**
 * Created by kevin on 17/03/15.
 */
@Service
class DownloaderSelector(val context: ApplicationContext, val downloaders: Set<Downloader>) {

    @Suppress("UNCHECKED_CAST")
    fun of(information: DownloadingInformation): Downloader =
            if (information.urls.isEmpty()) {
                NO_OP_DOWNLOADER
            } else {
                val d = downloaders.minBy { it.compatibility(information) }!!
                val clazz = (if (d is TargetClassAware) d.targetClass else d.javaClass) as Class<Downloader>
                context.getBean(clazz)
            }

    companion object {
        val NO_OP_DOWNLOADER = NoOpDownloader()
    }
}
