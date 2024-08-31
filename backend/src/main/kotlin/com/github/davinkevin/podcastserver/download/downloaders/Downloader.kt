package com.github.davinkevin.podcastserver.download.downloaders

import com.github.davinkevin.podcastserver.download.ItemDownloadManager


interface Downloader : Runnable {

    val downloadingInformation: DownloadingInformation

    fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager): Downloader

    fun download(): DownloadingItem

    fun startDownload()
    fun stopDownload()
    fun failDownload()
    fun finishDownload()

    fun compatibility(downloadingInformation: DownloadingInformation): Int
}
