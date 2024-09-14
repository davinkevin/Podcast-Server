package com.github.davinkevin.podcastserver.download.downloaders

import com.github.davinkevin.podcastserver.download.ItemDownloadManager


interface Downloader: Runnable {

    val downloadingInformation: DownloadingInformation

    fun download(): DownloadingItem

    override fun run() = startDownload()
    fun startDownload()
    fun stopDownload()
    fun failDownload()
    fun finishDownload()
}

interface DownloaderFactory {
    fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager): Downloader
    fun compatibility(downloadingInformation: DownloadingInformation): Int
}

