package com.github.davinkevin.podcastserver.manager.downloader

import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.manager.ItemDownloadManager
import lan.dk.podcastserver.manager.downloader.Downloader
import lan.dk.podcastserver.manager.downloader.DownloadingItem

/**
 * Created by kevin on 10/03/2016 for Podcast Server
 */
class NoOpDownloader : Downloader {

    private lateinit var downloadingItem: DownloadingItem
    private lateinit var itemDownloadManager: ItemDownloadManager

    override fun download(): Item? = Item.DEFAULT_ITEM
    override fun getItem(): Item? = Item.DEFAULT_ITEM
    override fun getItemUrl(item: Item): String? = item.url
    override fun startDownload() = failDownload()
    override fun pauseDownload() {}
    override fun restartDownload() {}
    override fun stopDownload() {}
    override fun finishDownload() {}
    override fun failDownload() = itemDownloadManager.removeACurrentDownload(downloadingItem.item)
    override fun compatibility(item: DownloadingItem) = -1
    override fun run() = startDownload()

    override fun setDownloadingItem(item: DownloadingItem) {
        this.downloadingItem = item
    }

    override fun setItemDownloadManager(itemDownloadManager: ItemDownloadManager) {
        this.itemDownloadManager = itemDownloadManager
    }
}
