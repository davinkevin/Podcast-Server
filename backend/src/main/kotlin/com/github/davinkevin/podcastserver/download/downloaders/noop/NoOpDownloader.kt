package com.github.davinkevin.podcastserver.download.downloaders.noop

import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.download.downloaders.Downloader
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem


/**
 * Created by kevin on 10/03/2016 for Podcast Server
 */
class NoOpDownloader : Downloader {

    override lateinit var downloadingInformation: DownloadingInformation
    private lateinit var itemDownloadManager: ItemDownloadManager

    override fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager): Downloader {
        this.downloadingInformation = information
        this.itemDownloadManager = itemDownloadManager
        return this
    }

    override fun download(): DownloadingItem = downloadingInformation.item
    override fun startDownload() = failDownload()
    override fun stopDownload() {}
    override fun finishDownload() {}
    override fun failDownload() = itemDownloadManager.removeACurrentDownload(downloadingInformation.item.id)
    override fun compatibility(downloadingInformation: DownloadingInformation) = -1
    override fun run() = startDownload()
}
