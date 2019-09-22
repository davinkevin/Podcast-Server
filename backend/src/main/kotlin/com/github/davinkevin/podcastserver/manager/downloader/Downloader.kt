package com.github.davinkevin.podcastserver.manager.downloader

import arrow.core.Some
import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils

interface Downloader : Runnable {

    val downloadingInformation: DownloadingInformation

    fun with(information: DownloadingInformation, itemDownloadManager: ItemDownloadManager)

    fun download(): DownloadingItem

    fun startDownload()
    fun pauseDownload()
    fun restartDownload() = this.startDownload()
    fun stopDownload()
    fun failDownload()
    fun finishDownload()

    fun compatibility(downloadingInformation: DownloadingInformation): Int
}
