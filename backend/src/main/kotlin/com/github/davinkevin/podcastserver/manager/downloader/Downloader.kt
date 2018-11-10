package com.github.davinkevin.podcastserver.manager.downloader

import arrow.core.Some
import arrow.core.getOrElse
import lan.dk.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils

interface Downloader : Runnable {

    val item: Item

    fun with(item: DownloadingItem, itemDownloadManager: ItemDownloadManager)

    fun download(): Item
    fun getItemUrl(item: Item): String
    fun getFileName(item: Item) = Some(getItemUrl(item))
            .map { StringUtils.substringBefore(it, "?") }
            .map{ FilenameUtils.getName(it) }
            .getOrElse { "" }!!

    fun startDownload()
    fun pauseDownload()
    fun restartDownload() = this.startDownload()
    fun stopDownload()
    fun failDownload()
    fun finishDownload()

    fun compatibility(downloadingItem: DownloadingItem): Int
}
