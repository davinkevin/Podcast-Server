package com.github.davinkevin.podcastserver.service.health

import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.stereotype.Component

/**
 * Created by kevin on 19/11/2017
 */
@Component
class DownloaderHealthIndicator(val itemDownloadManager: ItemDownloadManager) : AbstractHealthIndicator() {

    override fun doHealthCheck(builder: Health.Builder) {
        val waiting = itemDownloadManager.waitingQueue
        val items = itemDownloadManager.downloadingItems

        // @formatter:off
        builder.up()
            .withDetail("isDownloading", items.isNotEmpty())
            .withDetail("numberOfParallelDownloads", itemDownloadManager.limitParallelDownload)
            .withDetail("numberOfDownloading", items.size)
            .withDetail("downloadingItems", items)
            .withDetail("numberInQueue", waiting.size)
            .withDetail("waitingItems", waiting)
        .build()
        // @formatter:on
    }

}
