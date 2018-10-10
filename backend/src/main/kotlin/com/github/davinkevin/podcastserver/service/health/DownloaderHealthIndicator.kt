package com.github.davinkevin.podcastserver.service.health

import lan.dk.podcastserver.manager.ItemDownloadManager
import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.stereotype.Component

/**
 * Created by kevin on 19/11/2017
 */
@Component
class DownloaderHealthIndicator(val itemDownloadManager: ItemDownloadManager) : AbstractHealthIndicator() {

    override fun doHealthCheck(builder: Health.Builder) {
        // @formatter:off
        builder.up()
            .withDetail("isDownloading", itemDownloadManager.numberOfCurrentDownload > 0)
            .withDetail("numberOfParallelDownloads", itemDownloadManager.limitParallelDownload)
            .withDetail("numberOfDownloading", itemDownloadManager.numberOfCurrentDownload)
            .withDetail("downloadingItems", itemDownloadManager.itemsInDownloadingQueue)
            .withDetail("numberInQueue", itemDownloadManager.waitingQueue.length())
            .withDetail("waitingItems", itemDownloadManager.waitingQueue)
        .build()
        // @formatter:on
    }

}
