package com.github.davinkevin.podcastserver.manager.downloader

import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.manager.ItemDownloadManager

/**
 * Created by kevin on 10/03/2016 for Podcast Server
 */
class NoOpDownloader : Downloader {


    override val item: Item = Item.DEFAULT_ITEM
    private lateinit var downloadingItem: DownloadingItem
    private lateinit var itemDownloadManager: ItemDownloadManager

    override fun with(item: DownloadingItem, itemDownloadManager: ItemDownloadManager) {
        this.downloadingItem = item
        this.itemDownloadManager = itemDownloadManager
    }

    override fun download(): Item = Item.DEFAULT_ITEM
    override fun getItemUrl(item: Item): String = item.url
    override fun startDownload() = failDownload()
    override fun pauseDownload() {}
    override fun restartDownload() {}
    override fun stopDownload() {}
    override fun finishDownload() {}
    override fun failDownload() = itemDownloadManager.removeACurrentDownload(downloadingItem.item)
    override fun compatibility(downloadingItem: DownloadingItem) = -1
    override fun run() = startDownload()
}
