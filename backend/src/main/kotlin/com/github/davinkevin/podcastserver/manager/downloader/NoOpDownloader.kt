package com.github.davinkevin.podcastserver.manager.downloader

import com.github.davinkevin.podcastserver.manager.ItemDownloadManager

/**
 * Created by kevin on 10/03/2016 for Podcast Server
 */
class NoOpDownloader : Downloader {


    override lateinit var downloadingInformation: DownloadingInformation
    private lateinit var itemDownloadManager: ItemDownloadManager

    override fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager) {
        this.downloadingInformation = information
        this.itemDownloadManager = itemDownloadManager
    }

    override fun download(): DownloadingItem = downloadingInformation.item
    override fun startDownload() = failDownload()
    override fun stopDownload() {}
    override fun finishDownload() {}
    override fun failDownload() = itemDownloadManager.removeACurrentDownload(downloadingInformation.item.id)
    override fun compatibility(downloadingInformation: DownloadingInformation) = -1
    override fun run() = startDownload()
}
